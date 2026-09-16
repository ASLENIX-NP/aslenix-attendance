package com.aslenix.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "bs_date", nullable = false, length = 20)
    private String bsDate; // e.g. "2083-05-13"

    @Column(name = "bs_year", nullable = false)
    private int bsYear;

    @Column(name = "bs_month", nullable = false)
    private int bsMonth;

    @Column(name = "bs_day", nullable = false)
    private int bsDay;

    @Column(name = "ad_date")
    private LocalDate adDate;

    @Column(name = "is_holiday", nullable = false)
    private boolean holiday = false;

    @Column(length = 50)
    private String category = "GOVERNMENT";

    public CalendarEvent() {
    }

    public CalendarEvent(String title, String bsDate, int bsYear, int bsMonth, int bsDay, boolean holiday, String category) {
        this.title = title;
        this.bsDate = bsDate;
        this.bsYear = bsYear;
        this.bsMonth = bsMonth;
        this.bsDay = bsDay;
        this.holiday = holiday;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBsDate() {
        return bsDate;
    }

    public void setBsDate(String bsDate) {
        this.bsDate = bsDate;
    }

    public int getBsYear() {
        return bsYear;
    }

    public void setBsYear(int bsYear) {
        this.bsYear = bsYear;
    }

    public int getBsMonth() {
        return bsMonth;
    }

    public void setBsMonth(int bsMonth) {
        this.bsMonth = bsMonth;
    }

    public int getBsDay() {
        return bsDay;
    }

    public void setBsDay(int bsDay) {
        this.bsDay = bsDay;
    }

    public LocalDate getAdDate() {
        return adDate;
    }

    public void setAdDate(LocalDate adDate) {
        this.adDate = adDate;
    }

    public boolean isHoliday() {
        return holiday;
    }

    public void setHoliday(boolean holiday) {
        this.holiday = holiday;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
