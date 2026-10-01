package com.aslenix.attendance;

import com.aslenix.attendance.controller.LoginController;
import com.aslenix.attendance.controller.SetupController;
import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import com.aslenix.attendance.repository.UserRepository;
import com.aslenix.attendance.service.DataInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class InitialAdminSetupTest {

    @Autowired
    private SetupController setupController;

    @Autowired
    private LoginController loginController;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private OfficeSettingsRepository officeSettingsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.aslenix.attendance.repository.AdminRecoveryTokenRepository adminRecoveryTokenRepository;

    @BeforeEach
    void setUp() {
        // Clear tokens and admin users to simulate empty database state for setup testing
        adminRecoveryTokenRepository.deleteAll();

        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .toList();
        for (User admin : admins) {
            employeeRepository.findByUserUsername(admin.getUsername()).ifPresent(employeeRepository::delete);
            userRepository.delete(admin);
        }
    }

    @Test
    void testEmptyDatabaseRedirectsLoginAndHomeToSetup() {
        assertFalse(userRepository.existsByRole(Role.ADMIN), "Database must simulate 0 admins");

        // 1. /login redirects to /setup
        String loginView = loginController.login();
        assertEquals("redirect:/setup", loginView, "Empty database must redirect login page to /setup");

        // 2. Unauthenticated / redirects to /setup
        String homeView = loginController.home(null);
        assertEquals("redirect:/setup", homeView, "Empty database must redirect unauthenticated root to /setup");

        // 3. /setup displays setup template
        Model model = new ConcurrentModel();
        String setupView = setupController.showSetupPage(model);
        assertEquals("setup", setupView, "/setup must render setup.html when no admin exists");
    }

    @Test
    void testSetupValidationFailures() {
        assertFalse(userRepository.existsByRole(Role.ADMIN));

        // 1. Missing username
        RedirectAttributesModelMap r1 = new RedirectAttributesModelMap();
        String res1 = setupController.processSetup("", "pass12345", "pass12345", "Admin User", "admin@test.com", "IT", r1);
        assertEquals("redirect:/setup", res1);
        assertTrue(r1.getFlashAttributes().get("errorMessage").toString().contains("username is required"));

        // 2. Username too short
        RedirectAttributesModelMap r2 = new RedirectAttributesModelMap();
        String res2 = setupController.processSetup("ab", "pass12345", "pass12345", "Admin User", "admin@test.com", "IT", r2);
        assertEquals("redirect:/setup", res2);
        assertTrue(r2.getFlashAttributes().get("errorMessage").toString().contains("at least 3 characters"));

        // 3. Password too short
        RedirectAttributesModelMap r3 = new RedirectAttributesModelMap();
        String res3 = setupController.processSetup("newadmin", "123", "123", "Admin User", "admin@test.com", "IT", r3);
        assertEquals("redirect:/setup", res3);
        assertTrue(r3.getFlashAttributes().get("errorMessage").toString().contains("at least 6 characters"));

        // 4. Passwords do not match
        RedirectAttributesModelMap r4 = new RedirectAttributesModelMap();
        String res4 = setupController.processSetup("newadmin", "Secret1234", "Different1234", "Admin User", "admin@test.com", "IT", r4);
        assertEquals("redirect:/setup", res4);
        assertTrue(r4.getFlashAttributes().get("errorMessage").toString().contains("Passwords do not match"));

        // Verify still no admin created
        assertFalse(userRepository.existsByRole(Role.ADMIN));
    }

    @Test
    void testSuccessfulAdminSetupAndSystemInitialization() {
        assertFalse(userRepository.existsByRole(Role.ADMIN));

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String result = setupController.processSetup(
                "masteradmin",
                "SuperSecureAdmin2026!",
                "SuperSecureAdmin2026!",
                "Jane ChiefAdmin",
                "jane.admin@company.com",
                "Headquarters",
                redirect
        );

        // 1. Successfully redirects to login with setup=success
        assertEquals("redirect:/login?setup=success", result);

        // 2. Admin user exists in DB
        assertTrue(userRepository.existsByRole(Role.ADMIN));
        User adminUser = userRepository.findByUsername("masteradmin").orElseThrow();
        assertEquals(Role.ADMIN, adminUser.getRole());
        assertTrue(adminUser.isEnabled());
        assertFalse(adminUser.isPasswordChangeRequired());
        assertTrue(passwordEncoder.matches("SuperSecureAdmin2026!", adminUser.getPassword()));

        // 3. Department created
        Department dept = departmentRepository.findAll().stream()
                .filter(d -> "Headquarters".equals(d.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(dept, "Department 'Headquarters' should be created if not exists");

        // 4. Linked admin employee record created
        Employee adminEmp = employeeRepository.findByUserUsername("masteradmin").orElseThrow();
        assertEquals("Jane", adminEmp.getFirstName());
        assertEquals("ChiefAdmin", adminEmp.getLastName());
        assertEquals("jane.admin@company.com", adminEmp.getEmail());
        assertEquals("Super Administrator", adminEmp.getPosition());
        assertNotNull(adminEmp.getQrToken());
        assertTrue(adminEmp.isEnabled());

        // 5. OfficeSettings initialized
        assertTrue(officeSettingsRepository.count() > 0, "Office settings should be initialized");
        OfficeSettings settings = officeSettingsRepository.findFirstByOrderByIdAsc().orElseThrow();
        assertEquals("ASLENIX", settings.getOfficeName());
        assertNotNull(settings.getWorkStartTime());
        assertNotNull(settings.getWorkEndTime());
    }

    @Test
    void testSetupEndpointLockdownOnceAdminExists() {
        // Create an existing admin
        User existingAdmin = new User("existing_admin", passwordEncoder.encode("SecretPass123"), Role.ADMIN);
        existingAdmin.setEnabled(true);
        userRepository.save(existingAdmin);

        assertTrue(userRepository.existsByRole(Role.ADMIN));

        // 1. GET /setup is locked down and redirects to /login
        Model model = new ConcurrentModel();
        String view = setupController.showSetupPage(model);
        assertEquals("redirect:/login", view, "Once admin exists, GET /setup must redirect to /login");

        // 2. POST /setup is locked down and refuses to create another admin
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String postView = setupController.processSetup(
                "hacker_admin",
                "HackPass123",
                "HackPass123",
                "Infiltrator",
                "hack@test.com",
                "HQ",
                redirect
        );
        assertEquals("redirect:/login", postView, "Once admin exists, POST /setup must redirect to /login");
        assertFalse(userRepository.existsByUsername("hacker_admin"), "Must not create user when locked down");

        // 3. GET /login displays login form
        String loginView = loginController.login();
        assertEquals("login", loginView, "Once admin exists, /login renders the normal login view");
    }

    @Test
    void testDataInitializerProvisionAdminDirectly() {
        assertFalse(userRepository.existsByRole(Role.ADMIN));

        dataInitializer.provisionAdmin(
                "provisioned_admin",
                "DirectPass12345",
                "Robert Manager",
                "robert@aslenix.local"
        );

        assertTrue(userRepository.existsByRole(Role.ADMIN));
        User user = userRepository.findByUsername("provisioned_admin").orElseThrow();
        assertTrue(passwordEncoder.matches("DirectPass12345", user.getPassword()));

        Employee emp = employeeRepository.findByUserUsername("provisioned_admin").orElseThrow();
        assertEquals("Robert", emp.getFirstName());
        assertEquals("Manager", emp.getLastName());
    }
}
