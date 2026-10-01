package com.aslenix.attendance;

import com.aslenix.attendance.controller.AdminRecoveryController;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.UserRepository;
import com.aslenix.attendance.service.AdminRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminPasswordRecoveryFlowTest {

    @Autowired
    private AdminRecoveryService recoveryService;

    @Autowired
    private AdminRecoveryController recoveryController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private String persistentToken;

    @BeforeEach
    void setUp() {
        persistentToken = recoveryService.getPersistentToken();

        // Ensure an ADMIN user exists
        adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User("admin", passwordEncoder.encode("InitialAdmin123"), Role.ADMIN);
            return userRepository.save(u);
        });
        adminUser.setPassword(passwordEncoder.encode("InitialAdmin123"));
        adminUser.setEnabled(true);
        userRepository.save(adminUser);
    }

    @Test
    void testPersistentTokenLoadedFromEnvExample() {
        assertNotNull(persistentToken, "Persistent token must not be null");
        assertFalse(persistentToken.isBlank(), "Persistent token must be loaded from .env/.env.example");
    }

    @Test
    void testValidatePersistentTokenSuccess() {
        assertTrue(recoveryService.validateToken(persistentToken).isPresent(),
                "Persistent recovery token must be valid");
    }

    @Test
    void testValidateInvalidTokenFails() {
        assertTrue(recoveryService.validateToken("InvalidToken-123456").isEmpty(),
                "Invalid token must not be accepted");
    }

    @Test
    void testResetPasswordOnce() {
        String error = recoveryService.resetPassword(persistentToken, "NewAdminPassword999");
        assertNull(error, "Password reset should succeed without error");

        User updatedAdmin = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("NewAdminPassword999", updatedAdmin.getPassword()),
                "Admin password must be updated to new password");
    }

    @Test
    void testReusableRecoveryLink_CanBeUsedAgainAndAgain() {
        // First reset
        String error1 = recoveryService.resetPassword(persistentToken, "FirstResetPassword123");
        assertNull(error1);
        User adminAfterFirst = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("FirstResetPassword123", adminAfterFirst.getPassword()));

        // Check token is STILL VALID (not expired or consumed)
        assertTrue(recoveryService.validateToken(persistentToken).isPresent(),
                "Persistent token must remain valid after first use");

        // Second reset using the SAME link / token
        String error2 = recoveryService.resetPassword(persistentToken, "SecondResetPassword456");
        assertNull(error2);
        User adminAfterSecond = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("SecondResetPassword456", adminAfterSecond.getPassword()));

        // Check token is STILL VALID for future resets
        assertTrue(recoveryService.validateToken(persistentToken).isPresent(),
                "Persistent token must remain valid after second use");

        // Third reset using the SAME link / token
        String error3 = recoveryService.resetPassword(persistentToken, "ThirdResetPassword789");
        assertNull(error3);
        User adminAfterThird = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("ThirdResetPassword789", adminAfterThird.getPassword()));
    }

    @Test
    void testControllerRecoveryFlow() {
        Model model = new ConcurrentModel();

        // 1. GET /admin/recovery/{token}
        String view = recoveryController.showRecoveryForm(persistentToken, model);
        assertEquals("admin-recovery", view);
        assertEquals(false, model.getAttribute("tokenInvalid"));
        assertEquals(persistentToken, model.getAttribute("token"));

        // 2. GET /admin/recovery/invalid-token
        Model invalidModel = new ConcurrentModel();
        String invalidView = recoveryController.showRecoveryForm("badToken", invalidModel);
        assertEquals("admin-recovery", invalidView);
        assertEquals(true, invalidModel.getAttribute("tokenInvalid"));

        // 3. POST /admin/recovery/{token} with matching password
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String postResult = recoveryController.processRecovery(
                persistentToken,
                "BrandNewSecurePass2026",
                "BrandNewSecurePass2026",
                model,
                redirectAttributes
        );
        assertEquals("redirect:/login?recovery=success", postResult);

        User updatedAdmin = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("BrandNewSecurePass2026", updatedAdmin.getPassword()));

        // 4. POST /admin/recovery/{token} with mismatching password
        Model mismatchModel = new ConcurrentModel();
        RedirectAttributesModelMap mismatchRedirect = new RedirectAttributesModelMap();
        String mismatchResult = recoveryController.processRecovery(
                persistentToken,
                "PasswordOne123",
                "PasswordTwo456",
                mismatchModel,
                mismatchRedirect
        );
        assertEquals("admin-recovery", mismatchResult);
        assertEquals("Passwords do not match. Please verify.", mismatchModel.getAttribute("errorMessage"));
    }
}
