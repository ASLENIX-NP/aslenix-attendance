package com.aslenix.attendance;

import com.aslenix.attendance.controller.AdminDepartmentController;
import com.aslenix.attendance.controller.EmployeeController;
import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.EmployeeSequenceRepository;
import com.aslenix.attendance.repository.UserRepository;
import com.aslenix.attendance.security.CustomUserDetailsService;
import com.aslenix.attendance.service.DataInitializer;
import com.aslenix.attendance.service.EmployeeCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EmployeeCodeAndDepartmentTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeSequenceRepository employeeSequenceRepository;

    @Autowired
    private EmployeeCodeService employeeCodeService;

    @Autowired
    private EmployeeController employeeController;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private AdminDepartmentController adminDepartmentController;

    @Autowired
    private com.aslenix.attendance.service.EmployeeDeletionService employeeDeletionService;

    @BeforeEach
    void setUp() {
        for (Employee e : employeeRepository.findAll()) {
            if (e.getEmployeeCode() != null && !e.getEmployeeCode().startsWith("ADM-")) {
                employeeDeletionService.deleteEmployee(e.getId());
            }
        }
        employeeSequenceRepository.deleteAll();

        // Run department initialization
        dataInitializer.initializeDepartments();
    }

    @Test
    void testRemovalOfFullStackAndHrDepartments() {
        // Explicitly create legacy Full Stack and HR departments to simulate existing legacy DB
        departmentRepository.findByName("Full Stack").orElseGet(() -> {
            Department d = new Department();
            d.setName("Full Stack");
            d.setActive(true);
            return departmentRepository.save(d);
        });

        departmentRepository.findByName("HR").orElseGet(() -> {
            Department d = new Department();
            d.setName("HR");
            d.setActive(true);
            return departmentRepository.save(d);
        });

        // Verify findAvailableDepartments excludes them
        java.util.List<Department> available = departmentRepository.findAvailableDepartments();
        boolean hasFullStack = available.stream().anyMatch(d -> d.getName().equalsIgnoreCase("Full Stack"));
        boolean hasHr = available.stream().anyMatch(d -> d.getName().equalsIgnoreCase("HR"));
        assertFalse(hasFullStack, "Available departments must not include Full Stack");
        assertFalse(hasHr, "Available departments must not include HR");

        // Run cleanup
        dataInitializer.cleanupDeprecatedDepartments();

        // Verify they are removed or deactivated
        departmentRepository.findByName("Full Stack").ifPresent(d -> assertFalse(d.isActive()));
        departmentRepository.findByName("HR").ifPresent(d -> assertFalse(d.isActive()));
    }

    @Test
    void testStandardDepartmentsAndAbbreviations() {
        Department fe = departmentRepository.findByName("Frontend").orElseThrow();
        assertEquals("FE", fe.getEffectiveAbbreviation());

        Department uiux = departmentRepository.findByName("UI/UX").orElseThrow();
        assertEquals("FE", uiux.getEffectiveAbbreviation(),
                "UI/UX department must have abbreviation FE as specified");

        Department be = departmentRepository.findByName("Backend").orElseThrow();
        assertEquals("BE", be.getEffectiveAbbreviation());

        Department ad = departmentRepository.findByName("Administration").orElseThrow();
        assertEquals("AD", ad.getEffectiveAbbreviation());

        Department dm = departmentRepository.findByName("Digital Marketing").orElseThrow();
        assertEquals("DM", dm.getEffectiveAbbreviation());
    }

    @Test
    void testSequentialEmployeeGenerationAcrossDepartments() {
        // Reset test sequence to start from 1001 baseline
        employeeSequenceRepository.deleteAll();

        Department fe = departmentRepository.findByName("Frontend").orElseThrow();
        Department be = departmentRepository.findByName("Backend").orElseThrow();
        Department uiux = departmentRepository.findByName("UI/UX").orElseThrow();
        Department ad = departmentRepository.findByName("Administration").orElseThrow();
        Department dm = departmentRepository.findByName("Digital Marketing").orElseThrow();

        // 1. First employee -> Frontend -> FE-1001
        String code1 = employeeCodeService.generateNextEmployeeCode(fe);
        assertEquals("FE-1001", code1);

        // Save employee 1
        saveEmployeeWithCode(code1, fe, "emp1@aslenix.local");

        // 2. Second employee -> Backend -> BE-1002
        String code2 = employeeCodeService.generateNextEmployeeCode(be);
        assertEquals("BE-1002", code2);
        saveEmployeeWithCode(code2, be, "emp2@aslenix.local");

        // 3. Third employee -> UI/UX -> FE-1003
        String code3 = employeeCodeService.generateNextEmployeeCode(uiux);
        assertEquals("FE-1003", code3);
        saveEmployeeWithCode(code3, uiux, "emp3@aslenix.local");

        // 4. Fourth employee -> Administration -> AD-1004
        String code4 = employeeCodeService.generateNextEmployeeCode(ad);
        assertEquals("AD-1004", code4);
        saveEmployeeWithCode(code4, ad, "emp4@aslenix.local");

        // 5. Fifth employee -> Digital Marketing -> DM-1005
        String code5 = employeeCodeService.generateNextEmployeeCode(dm);
        assertEquals("DM-1005", code5);
        saveEmployeeWithCode(code5, dm, "emp5@aslenix.local");
    }

    @Test
    void testEmployeeCreationViaControllerMakesIdAndUsernameIdentical() {
        Department fe = departmentRepository.findByName("Frontend").orElseThrow();

        Employee emp = new Employee();
        emp.setFirstName("Alice");
        emp.setLastName("Wonder");
        emp.setEmail("alice.wonder@example.com");
        emp.setPosition("Frontend Engineer");
        emp.setJoiningDate(LocalDate.now());

        String redirect = employeeController.addEmployee(
                emp,
                null, // No manual username provided
                "TempPass123",
                fe.getId(),
                null
        );
        assertEquals("redirect:/admin/employees", redirect);

        assertNotNull(emp.getEmployeeCode());
        assertTrue(emp.getEmployeeCode().startsWith("FE-"));

        User user = emp.getUser();
        assertNotNull(user);
        assertEquals(emp.getEmployeeCode(), user.getUsername(),
                "Employee ID and login username must be 100% identical");

        // Verify login works with the generated username
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        assertNotNull(userDetails);
        assertEquals(user.getUsername(), userDetails.getUsername());

        // Verify case-insensitive login works as well
        UserDetails lowerDetails = userDetailsService.loadUserByUsername(user.getUsername().toLowerCase());
        assertNotNull(lowerDetails);
    }

    @Test
    void testAdminCannotManuallyModifyEmployeeCodeInEdit() {
        Department fe = departmentRepository.findByName("Frontend").orElseThrow();

        Employee emp = new Employee();
        emp.setFirstName("Bob");
        emp.setLastName("Builder");
        emp.setEmail("bob.builder@example.com");
        emp.setPosition("Developer");
        emp.setJoiningDate(LocalDate.now());

        employeeController.addEmployee(emp, null, "TempPass123", fe.getId(), null);
        String originalCode = emp.getEmployeeCode();
        assertNotNull(originalCode);

        // Attempt to edit and alter employee code
        Employee editForm = new Employee();
        editForm.setFirstName("Bob (Edited)");
        editForm.setLastName("Builder");
        editForm.setEmail("bob.builder@example.com");
        editForm.setPosition("Senior Developer");
        editForm.setJoiningDate(LocalDate.now());
        editForm.setEmployeeCode("HACKED-CODE-999");

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        employeeController.updateEmployee(
                emp.getId(),
                editForm,
                fe.getId(),
                null,
                null,
                false,
                redirectAttributes
        );

        Employee reloaded = employeeRepository.findById(emp.getId()).orElseThrow();
        assertEquals(originalCode, reloaded.getEmployeeCode(),
                "Admin should not be able to manually overwrite the generated employee ID");
        assertEquals("Senior Developer", reloaded.getPosition());
    }

    private void saveEmployeeWithCode(String code, Department dept, String email) {
        User user = new User();
        user.setUsername(code);
        user.setPassword("secret");
        user.setRole(Role.EMPLOYEE);
        user.setEnabled(true);
        userRepository.save(user);

        Employee emp = new Employee();
        emp.setEmployeeCode(code);
        emp.setEmail(email);
        emp.setFirstName("Test");
        emp.setLastName("Employee");
        emp.setPosition("Staff");
        emp.setDepartment(dept);
        emp.setUser(user);
        emp.setJoiningDate(LocalDate.now());
        emp.setEnabled(true);
        employeeRepository.save(emp);
    }

    @Test
    void testAdminCanAddAndRemoveDepartments() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // 1. Admin adds a new department "Quality Assurance" with abbreviation "QA"
        String redirect = adminDepartmentController.addDepartment("Quality Assurance", "QA", redirectAttributes);
        assertEquals("redirect:/admin/settings#departmentsSection", redirect);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("deptSuccess"));

        Department qa = departmentRepository.findByName("Quality Assurance").orElseThrow();
        assertEquals("QA", qa.getAbbreviation());
        assertTrue(qa.isActive());

        // Verify it appears in available departments
        java.util.List<Department> available = departmentRepository.findAvailableDepartments();
        assertTrue(available.stream().anyMatch(d -> d.getName().equals("Quality Assurance")));

        // Verify sequential employee ID generation works for QA
        String nextQaCode = employeeCodeService.generateNextEmployeeCode(qa);
        assertTrue(nextQaCode.startsWith("QA-"));

        // 2. Duplicate addition is prevented
        RedirectAttributesModelMap dupAttrs = new RedirectAttributesModelMap();
        adminDepartmentController.addDepartment("Quality Assurance", "QA", dupAttrs);
        assertTrue(dupAttrs.getFlashAttributes().containsKey("deptError"));

        // 3. Admin removes the department
        RedirectAttributesModelMap delAttrs = new RedirectAttributesModelMap();
        String delRedirect = adminDepartmentController.deleteDepartment(qa.getId(), null, delAttrs);
        assertEquals("redirect:/admin/settings#departmentsSection", delRedirect);
        assertTrue(delAttrs.getFlashAttributes().containsKey("deptSuccess"));

        // Verify it no longer appears in available departments
        java.util.List<Department> updatedAvailable = departmentRepository.findAvailableDepartments();
        assertFalse(updatedAvailable.stream().anyMatch(d -> d.getName().equals("Quality Assurance")));
    }

    @Test
    void testAdminCannotDeleteDepartmentWithActiveEmployeesWithoutReassignment() {
        Department be = departmentRepository.findByName("Backend").orElseThrow();

        // Save employee assigned to Backend
        saveEmployeeWithCode("BE-1001", be, "be.emp@aslenix.local");

        // Attempt deletion without reassignment
        RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();
        adminDepartmentController.deleteDepartment(be.getId(), null, attrs);

        assertTrue(attrs.getFlashAttributes().containsKey("deptError"));
        assertTrue(attrs.getFlashAttributes().get("deptError").toString().contains("assigned"));

        // Backend must still be active
        Department reloadedBe = departmentRepository.findById(be.getId()).orElseThrow();
        assertTrue(reloadedBe.isActive());
    }

    @Test
    void testAdminCanDeleteDepartmentWithEmployeeReassignment() {
        // Create temporary department
        Department temp = new Department("Mobile Development", "MD");
        temp.setActive(true);
        departmentRepository.save(temp);

        Department fe = departmentRepository.findByName("Frontend").orElseThrow();

        // Assign employee to temp
        saveEmployeeWithCode("MD-1001", temp, "mobile.dev@aslenix.local");
        Employee emp = employeeRepository.findByEmployeeCode("MD-1001").orElseThrow();
        assertEquals(temp.getId(), emp.getDepartment().getId());

        // Delete temp with reassignment to Frontend
        RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();
        adminDepartmentController.deleteDepartment(temp.getId(), fe.getId(), attrs);

        assertTrue(attrs.getFlashAttributes().containsKey("deptSuccess"));

        // Verify employee was reassigned to Frontend
        Employee reassigned = employeeRepository.findById(emp.getId()).orElseThrow();
        assertEquals(fe.getId(), reassigned.getDepartment().getId());

        // Verify temp is no longer available
        java.util.List<Department> available = departmentRepository.findAvailableDepartments();
        assertFalse(available.stream().anyMatch(d -> d.getName().equals("Mobile Development")));
    }

    @Test
    void testAdminCanReactivateDeletedDepartment() {
        Department dm = departmentRepository.findByName("Digital Marketing").orElseThrow();
        dm.setActive(false);
        departmentRepository.save(dm);

        assertFalse(departmentRepository.findAvailableDepartments().stream()
                .anyMatch(d -> d.getName().equals("Digital Marketing")));

        // Re-adding the department reactivates it
        RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();
        adminDepartmentController.addDepartment("Digital Marketing", "DM", attrs);

        assertTrue(attrs.getFlashAttributes().containsKey("deptSuccess"));
        assertTrue(attrs.getFlashAttributes().get("deptSuccess").toString().contains("reactivated"));

        Department reactivated = departmentRepository.findByName("Digital Marketing").orElseThrow();
        assertTrue(reactivated.isActive());
        assertTrue(departmentRepository.findAvailableDepartments().stream()
                .anyMatch(d -> d.getName().equals("Digital Marketing")));
    }
}
