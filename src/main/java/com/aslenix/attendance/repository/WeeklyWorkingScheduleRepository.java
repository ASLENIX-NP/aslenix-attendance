package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.WeeklyWorkingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyWorkingScheduleRepository
        extends JpaRepository<WeeklyWorkingSchedule, Long> {

    List<WeeklyWorkingSchedule>
    findByWeekStartOrderByWorkDateAsc(
            LocalDate weekStart
    );

    Optional<WeeklyWorkingSchedule>
    findByWeekStartAndWorkDate(
            LocalDate weekStart,
            LocalDate workDate
    );
}