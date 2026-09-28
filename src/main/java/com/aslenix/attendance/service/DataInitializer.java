package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {

        this.userRepository = userRepository;
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

        if (!userRepository.existsByUsername("admin")) {

            User admin = new User();

            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            admin.setPasswordChangeRequired(false);

            userRepository.save(admin);

            System.out.println("=================================");
            System.out.println("ASLENIX ADMIN CREATED");
            System.out.println("Username: admin");
            System.out.println("Password: 123");
            System.out.println("=================================");
        }
    }
}