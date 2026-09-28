package com.aslenix.attendance;

import com.aslenix.attendance.controller.EmployeeController;
import com.aslenix.attendance.controller.OfficeSettingsController;
import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminPasswordResetTest {

    @Autowired
    private EmployeeController employeeController;

    @Autowired
    private OfficeSettingsController officeSettingsController;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testAdminResetEmployeePasswordEndpoint() {
        // Create user & employee
        User user = new User("testemp_pwd", passwordEncoder.encode("oldPassword123"), Role.EMPLOYEE);
        user = userRepository.save(user);

        Employee emp = new Employee();
        emp.setFirstName("Alex");
        emp.setLastName("Test");
        emp.setEmail("alex.test@example.com");
        emp.setEmployeeCode("ASL-99881");
        emp.setJoiningDate(LocalDate.now());
        emp.setPosition("Developer");
        emp.setEnabled(true);
        emp.setUser(user);
        emp = employeeRepository.save(emp);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // 1. Reset with valid password
        String result = employeeController.resetEmployeePassword(emp.getId(), "newSecretPass2026", redirectAttributes);
        assertEquals("redirect:/admin/employees", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("successMessage"));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("newSecretPass2026", updatedUser.getPassword()),
                "User password must match newly reset password");
        assertFalse(passwordEncoder.matches("oldPassword123", updatedUser.getPassword()),
                "User password must not match old password");

        // 2. Reject short password
        RedirectAttributesModelMap shortRedirect = new RedirectAttributesModelMap();
        employeeController.resetEmployeePassword(emp.getId(), "123", shortRedirect);
        assertNotNull(shortRedirect.getFlashAttributes().get("errorMessage"),
                "Should reject password shorter than 6 characters");
    }

    @Test
    void testAdminUpdateEmployeePasswordDuringEdit() {
        Department dept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering");
            return departmentRepository.save(d);
        });

        User user = new User("editemp_user", passwordEncoder.encode("initialPass123"), Role.EMPLOYEE);
        user = userRepository.save(user);

        Employee emp = new Employee();
        emp.setFirstName("Morgan");
        emp.setLastName("Edit");
        emp.setEmail("morgan.edit@example.com");
        emp.setEmployeeCode("ASL-99882");
        emp.setJoiningDate(LocalDate.now());
        emp.setPosition("Designer");
        emp.setEnabled(true);
        emp.setUser(user);
        emp.setDepartment(dept);
        emp = employeeRepository.save(emp);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // Update employee details with new password
        emp.setPosition("Lead Designer");
        String result = employeeController.updateEmployee(
                emp.getId(),
                emp,
                dept.getId(),
                "brandNewPassword2026",
                null,
                false,
                redirectAttributes
        );

        assertEquals("redirect:/admin/employees", result);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("brandNewPassword2026", updatedUser.getPassword()),
                "Password must be updated when provided in edit form");
    }

    @Test
    void testAdminChangeOwnPassword() {
        // Ensure admin user exists
        User admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User a = new User("admin", passwordEncoder.encode("123"), Role.ADMIN);
            return userRepository.save(a);
        });
        admin.setPassword(passwordEncoder.encode("123"));
        userRepository.save(admin);

        Principal principal = () -> "admin";
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // 1. Wrong current password
        String resultFail = officeSettingsController.changeAdminPassword(
                "wrongOldPass",
                "brandNewAdminPass123",
                "brandNewAdminPass123",
                principal,
                redirectAttributes
        );
        assertEquals("redirect:/admin/settings", resultFail);
        assertNotNull(redirectAttributes.getFlashAttributes().get("errorMessage"));

        // 2. Passwords mismatch
        RedirectAttributesModelMap mismatchRedirect = new RedirectAttributesModelMap();
        officeSettingsController.changeAdminPassword(
                "123",
                "brandNewAdminPass123",
                "differentPassword123",
                principal,
                mismatchRedirect
        );
        assertNotNull(mismatchRedirect.getFlashAttributes().get("errorMessage"));

        // 3. Successful change
        RedirectAttributesModelMap successRedirect = new RedirectAttributesModelMap();
        String resultSuccess = officeSettingsController.changeAdminPassword(
                "123",
                "brandNewAdminPass123",
                "brandNewAdminPass123",
                principal,
                successRedirect
        );
        assertEquals("redirect:/admin/settings", resultSuccess);
        assertNotNull(successRedirect.getFlashAttributes().get("successMessage"));

        User updatedAdmin = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("brandNewAdminPass123", updatedAdmin.getPassword()),
                "Admin password must be updated to new password");
    }

    @Test
    void testTemplateContainsPasswordResetElements() throws Exception {
        String employeesHtml = Files.readString(Path.of("src/main/resources/templates/admin/employees.html"));
        String editEmployeeHtml = Files.readString(Path.of("src/main/resources/templates/admin/edit-employee.html"));
        String settingsHtml = Files.readString(Path.of("src/main/resources/templates/admin/settings.html"));

        // Employees list has Reset Password button & modal
        assertTrue(employeesHtml.contains("qr-btn-password"), "Employee cards must have Password button");
        assertTrue(employeesHtml.contains("id=\"resetPasswordModal\""), "Employee list must have Reset Password modal");
        assertTrue(employeesHtml.contains("openPasswordModal"), "Must have openPasswordModal function");
        assertTrue(employeesHtml.contains("generateRandomPassword"), "Must have password auto-generator");

        // Edit Employee has password reset fields
        assertTrue(editEmployeeHtml.contains("name=\"newPassword\""), "Edit employee must have newPassword input");
        assertTrue(editEmployeeHtml.contains("name=\"confirmNewPassword\""), "Edit employee must have confirmNewPassword input");

        // Settings has Admin Security section
        assertTrue(settingsHtml.contains("id=\"securitySection\""), "Settings must have security section");
        assertTrue(settingsHtml.contains("name=\"currentPassword\""), "Settings must have currentPassword input");
        assertTrue(settingsHtml.contains("name=\"newPassword\""), "Settings must have newPassword input");
        assertTrue(settingsHtml.contains("name=\"confirmPassword\""), "Settings must have confirmPassword input");
        assertTrue(settingsHtml.contains("/admin/settings/change-password"), "Settings must submit to change-password endpoint");
    }
}
