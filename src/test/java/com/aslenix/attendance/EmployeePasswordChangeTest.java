package com.aslenix.attendance;

import com.aslenix.attendance.controller.EmployeeController;
import com.aslenix.attendance.controller.EmployeePasswordController;
import com.aslenix.attendance.controller.LoginController;
import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.UserRepository;
import com.aslenix.attendance.security.PasswordChangeRequiredInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmployeePasswordChangeTest {

    @Autowired
    private EmployeePasswordController employeePasswordController;

    @Autowired
    private LoginController loginController;

    @Autowired
    private EmployeeController employeeController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordChangeRequiredInterceptor interceptor;

    private Department department;

    @BeforeEach
    void setUp() {
        department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering");
            d.setActive(true);
            return departmentRepository.save(d);
        });
    }

    private Employee createTestEmployee(String username, String rawPassword, boolean changeRequired) {
        User user = new User(username, passwordEncoder.encode(rawPassword), Role.EMPLOYEE);
        user.setPasswordChangeRequired(changeRequired);
        user = userRepository.save(user);

        Employee emp = new Employee();
        emp.setFirstName("First_" + username);
        emp.setLastName("Last_" + username);
        emp.setEmail(username + "@example.com");
        emp.setEmployeeCode("EMP-" + username.toUpperCase());
        emp.setJoiningDate(LocalDate.now());
        emp.setPosition("Developer");
        emp.setEnabled(true);
        emp.setDepartment(department);
        emp.setUser(user);
        return employeeRepository.save(emp);
    }

    @Test
    void testNewEmployeeCreationRequiresPasswordChange() {
        Employee newEmp = new Employee();
        newEmp.setFirstName("Brand");
        newEmp.setLastName("New");
        newEmp.setEmail("brand.new@example.com");
        newEmp.setPosition("Analyst");
        newEmp.setJoiningDate(LocalDate.now());

        employeeController.addEmployee(
                newEmp,
                "brand.new",
                "initialTempPass123",
                department.getId(),
                null
        );

        User createdUser = userRepository.findByUsername("brand.new").orElseThrow();
        assertTrue(createdUser.isPasswordChangeRequired(),
                "Newly created employee user must have passwordChangeRequired = true");
    }

    @Test
    void testAdminResetPasswordRequiresPasswordChange() {
        Employee emp = createTestEmployee("emp_to_reset", "OldPass123", false);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        employeeController.resetEmployeePassword(emp.getId(), "AdminTempPass456", redirectAttributes);

        User updatedUser = userRepository.findById(emp.getUser().getId()).orElseThrow();
        assertTrue(updatedUser.isPasswordChangeRequired(),
                "After admin resets password, passwordChangeRequired must be true");
    }

    @Test
    void testFirstLoginRedirectsToChangePassword() {
        Employee emp = createTestEmployee("emp_first_login", "TempPass123", true);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                emp.getUser().getUsername(),
                "TempPass123",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 1. Home route redirect
        String homeRedirect = loginController.home(auth);
        assertEquals("redirect:/employee/change-password", homeRedirect,
                "First login must redirect employee to /employee/change-password");

        // 2. Interceptor blocks /employee/dashboard
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/employee/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = false;
        try {
            allowed = interceptor.preHandle(request, response, new Object());
        } catch (Exception e) {
            fail("Interceptor threw exception: " + e.getMessage());
        }

        assertFalse(allowed, "Interceptor must block navigation when password change is required");
        assertEquals("/employee/change-password", response.getRedirectedUrl());

        // 3. Interceptor allows /employee/change-password
        MockHttpServletRequest allowRequest = new MockHttpServletRequest("GET", "/employee/change-password");
        MockHttpServletResponse allowResponse = new MockHttpServletResponse();
        boolean changeAllowed = false;
        try {
            changeAllowed = interceptor.preHandle(allowRequest, allowResponse, new Object());
        } catch (Exception e) {
            fail("Interceptor threw exception: " + e.getMessage());
        }
        assertTrue(changeAllowed, "Interceptor must allow accessing /employee/change-password");
    }

    @Test
    void testEmployeeChangePasswordSuccessFlow() {
        Employee emp = createTestEmployee("emp_change_flow", "CurrentPass123", true);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                emp.getUser().getUsername(),
                "CurrentPass123",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Render page
        Model model = new ConcurrentModel();
        String view = employeePasswordController.showChangePasswordPage(auth, model);
        assertEquals("employee/change-password", view);
        assertTrue((Boolean) model.getAttribute("isFirstLogin"), "Should indicate first login");

        // Attempt 1: Wrong current password
        RedirectAttributesModelMap r1 = new RedirectAttributesModelMap();
        employeePasswordController.processChangePassword(
                "WrongCurrent",
                "NewSecurePass2026",
                "NewSecurePass2026",
                null,
                auth,
                r1
        );
        assertNotNull(r1.getFlashAttributes().get("errorMessage"), "Should fail on wrong current password");

        // Attempt 2: Mismatched confirm password
        RedirectAttributesModelMap r2 = new RedirectAttributesModelMap();
        employeePasswordController.processChangePassword(
                "CurrentPass123",
                "NewSecurePass2026",
                "DifferentConfirm2026",
                null,
                auth,
                r2
        );
        assertNotNull(r2.getFlashAttributes().get("errorMessage"), "Should fail on mismatched confirmation");

        // Attempt 3: Same as current password
        RedirectAttributesModelMap r3 = new RedirectAttributesModelMap();
        employeePasswordController.processChangePassword(
                "CurrentPass123",
                "CurrentPass123",
                "CurrentPass123",
                null,
                auth,
                r3
        );
        assertNotNull(r3.getFlashAttributes().get("errorMessage"), "Should fail when new password equals current");

        // Attempt 4: Successful password change
        RedirectAttributesModelMap r4 = new RedirectAttributesModelMap();
        String dest = employeePasswordController.processChangePassword(
                "CurrentPass123",
                "BrandNewSecurePass2026",
                "BrandNewSecurePass2026",
                null,
                auth,
                r4
        );
        assertEquals("redirect:/employee/dashboard", dest);
        assertNotNull(r4.getFlashAttributes().get("successMessage"));

        // Verify in DB
        User updated = userRepository.findById(emp.getUser().getId()).orElseThrow();
        assertFalse(updated.isPasswordChangeRequired(), "passwordChangeRequired must be false after update");
        assertTrue(passwordEncoder.matches("BrandNewSecurePass2026", updated.getPassword()),
                "Database password must match new password");

        // Interceptor now allows accessing /employee/dashboard
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/employee/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowedAfterChange = false;
        try {
            allowedAfterChange = interceptor.preHandle(request, response, new Object());
        } catch (Exception e) {
            fail(e);
        }
        assertTrue(allowedAfterChange, "Interceptor must now allow employee into dashboard");
    }

    @Test
    void testEmployeeCanChangePasswordAnytimeFromProfile() {
        Employee emp = createTestEmployee("emp_anytime", "ExistingPass123", false);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                emp.getUser().getUsername(),
                "ExistingPass123",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String result = employeePasswordController.processChangePassword(
                "ExistingPass123",
                "UpdatedPassJuly2026",
                "UpdatedPassJuly2026",
                "profile",
                auth,
                redirect
        );

        assertEquals("redirect:/employee/profile", result, "When submitted from profile, redirects back to profile");
        assertNotNull(redirect.getFlashAttributes().get("successMessage"));

        User user = userRepository.findById(emp.getUser().getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("UpdatedPassJuly2026", user.getPassword()));
    }
}
