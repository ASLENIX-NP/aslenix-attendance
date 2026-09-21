package com.aslenix.attendance.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // ============================================================
    // PASSWORD ENCODER
    // ============================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ============================================================
    // SECURITY FILTER CHAIN
    // ============================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

            // ====================================================
            // AUTHORIZATION
            // ====================================================

            .authorizeHttpRequests(auth -> auth

                // ------------------------------------------------
                // PUBLIC
                // ------------------------------------------------

                .requestMatchers(
                        "/login",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/uploads/**"
                ).permitAll()

                // ------------------------------------------------
                // ADMIN
                // ------------------------------------------------

                .requestMatchers("/admin/**")
                .hasRole("ADMIN")

                // ------------------------------------------------
                // EMPLOYEE
                // ------------------------------------------------

                .requestMatchers("/employee/**")
                .hasRole("EMPLOYEE")

                // ------------------------------------------------
                // ROOT
                // ------------------------------------------------

                .requestMatchers("/")
                .authenticated()

                // ------------------------------------------------
                // EVERYTHING ELSE
                // ------------------------------------------------

                .anyRequest()
                .authenticated()
            )

            // ====================================================
            // FORM LOGIN
            // ====================================================

            .formLogin(form -> form

                .loginPage("/login")

                /*
                 * Send every successfully authenticated user to /
                 *
                 * LoginController then decides whether the user
                 * is ADMIN or EMPLOYEE.
                 */
                .defaultSuccessUrl("/", true)

                .permitAll()
            )

            // ====================================================
            // LOGOUT
            // ====================================================

            .logout(logout -> logout

                .logoutSuccessUrl("/login?logout")

                .permitAll()
            );

        return http.build();
    }
}