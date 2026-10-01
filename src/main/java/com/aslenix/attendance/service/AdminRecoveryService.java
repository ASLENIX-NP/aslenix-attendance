package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.AdminRecoveryToken;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.AdminRecoveryTokenRepository;
import com.aslenix.attendance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminRecoveryService {

    private static final Logger log =
            LoggerFactory.getLogger(AdminRecoveryService.class);

    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final AdminRecoveryTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.recovery-token:}")
    private String configuredToken;

    public AdminRecoveryService(
            AdminRecoveryTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ================================================================
    // PERSISTENT TOKEN RESOLUTION (.env.example / .env / Environment)
    // ================================================================

    /**
     * Resolves the persistent admin recovery token.
     * Looks in:
     * 1. Spring property `app.admin.recovery-token`
     * 2. OS environment variable `ADMIN_RECOVERY_TOKEN`
     * 3. Local `.env` or `.env.example` files (kept secret via .gitignore)
     */
    public String getPersistentToken() {
        if (configuredToken != null && !configuredToken.trim().isEmpty()) {
            return configuredToken.trim();
        }

        String sysEnv = System.getenv("ADMIN_RECOVERY_TOKEN");
        if (sysEnv != null && !sysEnv.trim().isEmpty()) {
            return sysEnv.trim();
        }

        Map<String, String> dotEnv = loadDotEnv();
        if (dotEnv.containsKey("ADMIN_RECOVERY_TOKEN") && !dotEnv.get("ADMIN_RECOVERY_TOKEN").trim().isEmpty()) {
            return dotEnv.get("ADMIN_RECOVERY_TOKEN").trim();
        }

        if (dotEnv.containsKey("ADMIN_RECOVERY_URL")) {
            String url = dotEnv.get("ADMIN_RECOVERY_URL").trim();
            int idx = url.lastIndexOf('/');
            if (idx != -1 && idx < url.length() - 1) {
                return url.substring(idx + 1).trim();
            }
        }

        return "";
    }

    /**
     * Constant-time check if the provided raw token matches the persistent recovery token.
     */
    public boolean isPersistentToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String persistentToken = getPersistentToken();
        if (persistentToken == null || persistentToken.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                persistentToken.getBytes(StandardCharsets.UTF_8),
                rawToken.trim().getBytes(StandardCharsets.UTF_8)
        );
    }

    // ================================================================
    // GENERATE RECOVERY TOKEN
    // ================================================================

    /**
     * If a persistent token is configured in .env.example / environment,
     * returns it so the admin can use the same link repeatedly.
     * Otherwise generates a cryptographically secure random token and stores its SHA-256 hash.
     */
    @Transactional
    public Optional<String> generateRecoveryToken() {
        String persistentToken = getPersistentToken();
        if (persistentToken != null && !persistentToken.isBlank()) {
            log.info("Using configured persistent admin recovery token.");
            return Optional.of(persistentToken);
        }

        Optional<User> adminOpt = findAdminUser();
        if (adminOpt.isEmpty()) {
            log.warn("No ADMIN user found for recovery token generation.");
            return Optional.empty();
        }

        User admin = adminOpt.get();

        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = sha256(rawToken);

        AdminRecoveryToken recoveryToken = new AdminRecoveryToken();
        recoveryToken.setTokenHash(tokenHash);
        recoveryToken.setUser(admin);
        recoveryToken.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        recoveryToken.setUsed(false);
        recoveryToken.setCreatedAt(LocalDateTime.now());

        tokenRepository.save(recoveryToken);

        log.info("Admin recovery token generated for user '{}'. Expires at {}.",
                admin.getUsername(), recoveryToken.getExpiresAt());

        return Optional.of(rawToken);
    }

    // ================================================================
    // VALIDATE TOKEN
    // ================================================================

    /**
     * Validates a raw token:
     * 1. Checks if it matches the persistent token (reusable, never expires).
     * 2. Otherwise checks dynamic database-backed tokens.
     */
    @Transactional(readOnly = true)
    public Optional<AdminRecoveryToken> validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        // --- Check Persistent Token (can be used repeatedly) ---
        if (isPersistentToken(rawToken)) {
            Optional<User> adminOpt = findAdminUser();
            if (adminOpt.isPresent()) {
                AdminRecoveryToken virtualToken = new AdminRecoveryToken();
                virtualToken.setUser(adminOpt.get());
                virtualToken.setTokenHash(sha256(rawToken));
                virtualToken.setExpiresAt(LocalDateTime.now().plusYears(100)); // never expires
                virtualToken.setUsed(false); // never consumed
                virtualToken.setCreatedAt(LocalDateTime.now());
                return Optional.of(virtualToken);
            } else {
                log.warn("Persistent recovery token used but no ADMIN user exists yet in database.");
                return Optional.empty();
            }
        }

        // --- Check Database Tokens ---
        String tokenHash = sha256(rawToken);
        Optional<AdminRecoveryToken> tokenOpt = tokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        AdminRecoveryToken token = tokenOpt.get();

        if (token.isUsed() || token.isExpired()) {
            return Optional.empty();
        }

        if (token.getUser().getRole() != Role.ADMIN || !token.getUser().isEnabled()) {
            return Optional.empty();
        }

        return Optional.of(token);
    }

    // ================================================================
    // RESET PASSWORD
    // ================================================================

    /**
     * Resets the admin password:
     * - If using persistent token: updates admin password and LEAVES the token active
     *   so the same link can be used again and again.
     * - If using a one-time DB token: marks token as used.
     */
    @Transactional
    public String resetPassword(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return "Password must be at least 6 characters long.";
        }

        // --- Handle Persistent Token (Reusable) ---
        if (isPersistentToken(rawToken)) {
            Optional<User> adminOpt = findAdminUser();
            if (adminOpt.isEmpty()) {
                return "No administrator account exists to reset.";
            }

            User admin = adminOpt.get();
            admin.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(admin);

            log.info("Admin password reset successfully via reusable persistent token for user '{}'.",
                    admin.getUsername());

            return null; // success, persistent token stays valid for next time!
        }

        // --- Handle Database One-Time Token ---
        String tokenHash = sha256(rawToken);
        Optional<AdminRecoveryToken> tokenOpt = tokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            return "Recovery link is invalid or expired.";
        }

        AdminRecoveryToken token = tokenOpt.get();

        if (token.isUsed() || token.isExpired()) {
            return "Recovery link is invalid or expired.";
        }

        if (token.getUser().getRole() != Role.ADMIN || !token.getUser().isEnabled()) {
            return "Recovery link is invalid or expired.";
        }

        User admin = token.getUser();
        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Admin password reset successfully via one-time recovery token for user '{}'.",
                admin.getUsername());

        return null;
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    private Optional<User> findAdminUser() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.isEnabled())
                .findFirst()
                .or(() -> userRepository.findAll().stream()
                        .filter(u -> u.getRole() == Role.ADMIN)
                        .findFirst());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> map = new HashMap<>();
        File[] candidates = new File[]{
                new File(".env.example"),
                new File(".env"),
                new File("../.env.example"),
                new File("../.env"),
                new File("aslenix-attendance/.env.example"),
                new File("aslenix-attendance/.env")
        };
        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eq = line.indexOf('=');
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        map.putIfAbsent(key, val);
                    }
                } catch (Exception e) {
                    // Ignore read errors
                }
            }
        }
        return map;
    }
}
