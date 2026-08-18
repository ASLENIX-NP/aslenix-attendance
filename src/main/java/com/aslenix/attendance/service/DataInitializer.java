package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

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