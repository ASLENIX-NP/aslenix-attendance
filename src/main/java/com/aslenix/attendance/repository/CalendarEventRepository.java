package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByBsYearAndBsMonthOrderByBsDayAsc(int bsYear, int bsMonth);

    List<CalendarEvent> findAllByOrderByBsDateAsc();

    boolean existsByBsDateAndTitle(String bsDate, String title);
}
