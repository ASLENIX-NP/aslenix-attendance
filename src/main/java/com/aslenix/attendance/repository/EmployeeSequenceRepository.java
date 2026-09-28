package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.EmployeeSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeSequenceRepository extends JpaRepository<EmployeeSequence, String> {
}
