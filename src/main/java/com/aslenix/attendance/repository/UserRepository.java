package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    long countByRole(Role role);
}