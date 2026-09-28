package com.aslenix.attendance.security;

import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public PasswordChangeRequiredInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        String uri = request.getRequestURI();

        // Allow change-password endpoints, logout, login, and static assets
        if (uri.startsWith("/employee/change-password")
                || uri.startsWith("/logout")
                || uri.startsWith("/login")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || uri.startsWith("/uploads/")) {
            return true;
        }

        // Only enforce for employees with ROLE_EMPLOYEE
        boolean isEmployee = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_EMPLOYEE".equals(a.getAuthority()));

        if (isEmployee) {
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null && user.isPasswordChangeRequired()) {
                response.sendRedirect(request.getContextPath() + "/employee/change-password");
                return false;
            }
        }

        return true;
    }
}
