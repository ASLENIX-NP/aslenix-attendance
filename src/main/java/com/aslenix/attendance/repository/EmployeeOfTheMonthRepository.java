package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.EmployeeOfTheMonth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeOfTheMonthRepository
        extends JpaRepository<EmployeeOfTheMonth, Long> {

    Optional<EmployeeOfTheMonth> findByEvalYearAndEvalMonth(int evalYear, int evalMonth);

    Optional<EmployeeOfTheMonth> findTopByOrderByEvalYearDescEvalMonthDesc();

    List<EmployeeOfTheMonth> findAllByOrderByEvalYearDescEvalMonthDesc();
}
