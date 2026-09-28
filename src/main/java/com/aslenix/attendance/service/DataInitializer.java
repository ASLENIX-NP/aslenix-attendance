package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import com.aslenix.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficeSettingsRepository officeSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.setup.auto-create-admin:false}")
    private boolean autoCreateAdmin;

    @Value("${app.setup.initial-admin-username:admin}")
    private String initialAdminUsername;

    @Value("${app.setup.initial-admin-password:}")
    private String initialAdminPassword;

    @Value("${app.setup.initial-admin-name:System Administrator}")
    private String initialAdminName;

    @Value("${app.setup.initial-admin-email:admin@aslenix.local}")
    private String initialAdminEmail;

    public DataInitializer(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            OfficeSettingsRepository officeSettingsRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {

        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.officeSettingsRepository = officeSettingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {

        // Ensure MySQL column nullability allows detached references on employee deletion
        try {
            jdbcTemplate.execute("ALTER TABLE tasks MODIFY COLUMN employee_id BIGINT NULL");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE task_assignments MODIFY COLUMN employee_id BIGINT NULL");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE task_assignment_histories MODIFY COLUMN updated_by_id BIGINT NULL");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE departments ADD COLUMN abbreviation VARCHAR(20) NULL");
        } catch (Exception ignored) {
        }

        // Initialize standard departments with abbreviations
        initializeDepartments();

        // Safely remove deprecated departments (Full Stack, HR)
        try {
            jdbcTemplate.execute("UPDATE employees SET department_id = (SELECT id FROM departments WHERE name = 'Backend' LIMIT 1) WHERE department_id IN (SELECT id FROM (SELECT id FROM departments WHERE LOWER(name) IN ('full stack', 'fullstack', 'full-stack')) AS tmp)");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("UPDATE employees SET department_id = (SELECT id FROM departments WHERE name = 'Administration' LIMIT 1) WHERE department_id IN (SELECT id FROM (SELECT id FROM departments WHERE LOWER(name) = 'hr') AS tmp)");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("DELETE FROM departments WHERE LOWER(name) IN ('full stack', 'hr', 'fullstack', 'full-stack')");
        } catch (Exception ignored) {
        }

        cleanupDeprecatedDepartments();

        // Check for CLI argument-based initial admin provisioning
        String cliUsername = null;
        String cliPassword = null;
        for (String arg : args) {
            if (arg.startsWith("--admin-username=")) {
                cliUsername = arg.substring("--admin-username=".length()).trim();
            } else if (arg.startsWith("--admin-password=")) {
                cliPassword = arg.substring("--admin-password=".length()).trim();
            } else if (arg.startsWith("--admin-user=")) {
                cliUsername = arg.substring("--admin-user=".length()).trim();
            } else if (arg.startsWith("--admin-pass=")) {
                cliPassword = arg.substring("--admin-pass=".length()).trim();
            }
        }

        boolean hasAdmin = userRepository.existsByRole(Role.ADMIN);

        // 1. If CLI credentials provided on empty database, provision admin
        if (!hasAdmin && cliUsername != null && !cliUsername.isBlank() && cliPassword != null && !cliPassword.isBlank()) {
            provisionAdmin(cliUsername, cliPassword, initialAdminName, initialAdminEmail);
            return;
        }

        // 2. If environment/properties explicitly configure auto-provisioning
        if (!hasAdmin && (autoCreateAdmin || (initialAdminPassword != null && !initialAdminPassword.isBlank()))) {
            String passwordToUse = (initialAdminPassword != null && !initialAdminPassword.isBlank())
                    ? initialAdminPassword
                    : "123";
            provisionAdmin(initialAdminUsername, passwordToUse, initialAdminName, initialAdminEmail);
            return;
        }

        // 3. Otherwise, if database is empty of admins, log instructions for Web Setup Wizard
        if (!hasAdmin) {
            System.out.println("=====================================================================");
            System.out.println("  ASLENIX ATTENDANCE: EMPTY DATABASE DETECTED");
            System.out.println("  No administrator account found.");
            System.out.println("  -> Open your browser at: http://localhost:8080/setup");
            System.out.println("     to complete the initial Super Admin account setup.");
            System.out.println("  -> Or set AUTO_CREATE_ADMIN=true in .env to auto-seed.");
            System.out.println("=====================================================================");
        }
    }

    public void provisionAdmin(String username, String rawPassword, String fullName, String email) {
        User admin = new User();
        admin.setUsername(username.trim());
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin.setPasswordChangeRequired(false);
        userRepository.save(admin);

        if (officeSettingsRepository.count() == 0) {
            OfficeSettings settings = new OfficeSettings();
            settings.setOfficeName("ASLENIX");
            settings.setOfficeLocation("ASLENIX Headquarters");
            settings.setLatitude(27.68777628544264);
            settings.setLongitude(85.3303680512975);
            settings.setAllowedRadiusMeters(100.0);
            settings.setWorkStartTime(LocalTime.of(10, 0));
            settings.setWorkEndTime(LocalTime.of(18, 0));
            settings.setLateGraceMinutes(15);
            settings.setSunday(true);
            settings.setMonday(true);
            settings.setTuesday(true);
            settings.setWednesday(true);
            settings.setThursday(true);
            settings.setFriday(true);
            settings.setSaturday(false);
            officeSettingsRepository.save(settings);
        }

        Department department = departmentRepository.findByName("Administration").orElseGet(() -> {
            Department d = new Department();
            d.setName("Administration");
            d.setAbbreviation("AD");
            d.setActive(true);
            return departmentRepository.save(d);
        });
        if (department.getAbbreviation() == null || department.getAbbreviation().isBlank()) {
            department.setAbbreviation("AD");
            departmentRepository.save(department);
        }

        String name = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "System Admin";
        String[] parts = name.split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "Administrator";
        String adminEmail = (email != null && !email.isBlank()) ? email.trim() : (username.trim() + "@aslenix.local");

        if (employeeRepository.findByEmail(adminEmail).isEmpty()) {
            Employee adminEmp = new Employee();
            adminEmp.setUser(admin);
            adminEmp.setFirstName(firstName);
            adminEmp.setLastName(lastName);
            adminEmp.setEmail(adminEmail);
            adminEmp.setEmployeeCode("ADM-001");
            adminEmp.setQrToken(UUID.randomUUID().toString());
            adminEmp.setPosition("Super Administrator");
            adminEmp.setJoiningDate(LocalDate.now());
            adminEmp.setEnabled(true);
            adminEmp.setDepartment(department);
            employeeRepository.save(adminEmp);
        }

        System.out.println("=====================================================================");
        System.out.println("  ASLENIX ADMIN PROVISIONED");
        System.out.println("  Username: " + username);
        System.out.println("  Role: " + Role.ADMIN);
        System.out.println("=====================================================================");
    }

    public void initializeDepartments() {
        if (departmentRepository.count() == 0) {
            seedDepartment("Frontend", "FE");
            seedDepartment("UI/UX", "FE");
            seedDepartment("Backend", "BE");
            seedDepartment("Administration", "AD");
            seedDepartment("Digital Marketing", "DM");
        } else {
            updateAbbreviationIfMissing("Frontend", "FE");
            updateAbbreviationIfMissing("UI/UX", "FE");
            updateAbbreviationIfMissing("Backend", "BE");
            updateAbbreviationIfMissing("Administration", "AD");
            updateAbbreviationIfMissing("Digital Marketing", "DM");
        }
    }

    private void seedDepartment(String name, String abbreviation) {
        Department dept = departmentRepository.findByName(name).orElseGet(() -> {
            Department d = new Department();
            d.setName(name);
            d.setActive(true);
            return d;
        });
        dept.setAbbreviation(abbreviation);
        departmentRepository.save(dept);
    }

    private void updateAbbreviationIfMissing(String name, String abbreviation) {
        departmentRepository.findByName(name).ifPresent(dept -> {
            if (dept.getAbbreviation() == null || dept.getAbbreviation().isBlank()) {
                dept.setAbbreviation(abbreviation);
                departmentRepository.save(dept);
            }
        });
    }

    public void cleanupDeprecatedDepartments() {
        Department backend = departmentRepository.findByName("Backend").orElse(null);
        Department administration = departmentRepository.findByName("Administration").orElse(null);

        removeDepartmentSafely("Full Stack", backend);
        removeDepartmentSafely("HR", administration);
        removeDepartmentSafely("Fullstack", backend);
        removeDepartmentSafely("Full-Stack", backend);
    }

    private void removeDepartmentSafely(String name, Department fallback) {
        departmentRepository.findByName(name).ifPresent(dept -> {
            try {
                List<Employee> allEmployees = employeeRepository.findAll();
                for (Employee emp : allEmployees) {
                    if (emp.getDepartment() != null && emp.getDepartment().getId().equals(dept.getId())) {
                        emp.setDepartment(fallback);
                        employeeRepository.save(emp);
                    }
                }
                departmentRepository.delete(dept);
            } catch (Exception e) {
                dept.setActive(false);
                departmentRepository.save(dept);
            }
        });
    }
}