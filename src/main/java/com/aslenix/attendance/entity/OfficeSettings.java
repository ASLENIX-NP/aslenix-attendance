package com.aslenix.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "office_settings")
public class OfficeSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String officeName;

    @Column(nullable = false, length = 255)
    private String officeLocation;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double allowedRadiusMeters;

    /*
     * Working hours
     *
     * These values are stored permanently in the
     * office_settings table.
     *
     * Defaults are provided so that a new settings
     * record is never created with blank working hours.
     */
    @Column(nullable = false)
    private LocalTime workStartTime = LocalTime.of(10, 0);

    @Column(nullable = false)
    private LocalTime workEndTime = LocalTime.of(18, 0);

    /*
     * Number of minutes after the normal start time
     * before an employee is considered late.
     */
    @Column(nullable = false)
    private int lateGraceMinutes = 15;

    /*
     * Working days
     */
    @Column(nullable = false)
    private boolean sunday = true;

    @Column(nullable = false)
    private boolean monday = true;

    @Column(nullable = false)
    private boolean tuesday = true;

    @Column(nullable = false)
    private boolean wednesday = true;

    @Column(nullable = false)
    private boolean thursday = true;

    @Column(nullable = false)
    private boolean friday = true;

    @Column(nullable = false)
    private boolean saturday = false;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public OfficeSettings() {
        /*
         * Defaults are intentionally kept here as well.
         * This protects newly created Java objects from
         * having null working hours.
         */
        this.workStartTime = LocalTime.of(10, 0);
        this.workEndTime = LocalTime.of(18, 0);
        this.lateGraceMinutes = 15;
    }


    /*
     * =========================================================
     * ID
     * =========================================================
     */

    public Long getId() {
        return id;
    }


    /*
     * =========================================================
     * OFFICE NAME
     * =========================================================
     */

    public String getOfficeName() {
        return officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
    }


    /*
     * =========================================================
     * OFFICE LOCATION
     * =========================================================
     */

    public String getOfficeLocation() {
        return officeLocation;
    }

    public void setOfficeLocation(String officeLocation) {
        this.officeLocation = officeLocation;
    }


    /*
     * =========================================================
     * LATITUDE
     * =========================================================
     */

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }


    /*
     * =========================================================
     * LONGITUDE
     * =========================================================
     */

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }


    /*
     * =========================================================
     * ATTENDANCE RADIUS
     * =========================================================
     */

    public double getAllowedRadiusMeters() {
        return allowedRadiusMeters;
    }

    public void setAllowedRadiusMeters(double allowedRadiusMeters) {
        this.allowedRadiusMeters = allowedRadiusMeters;
    }

    /*
     * Compatibility method.
     *
     * Older code may use getAttendanceRadius().
     * It returns the same value as allowedRadiusMeters.
     */
    public double getAttendanceRadius() {
        return allowedRadiusMeters;
    }


    /*
     * =========================================================
     * WORK START TIME
     * =========================================================
     */

    public LocalTime getWorkStartTime() {
        return workStartTime;
    }

    public void setWorkStartTime(LocalTime workStartTime) {

        /*
         * Never allow the working start time to become null.
         */
        if (workStartTime == null) {
            this.workStartTime = LocalTime.of(10, 0);
        } else {
            this.workStartTime = workStartTime;
        }
    }


    /*
     * =========================================================
     * WORK END TIME
     * =========================================================
     */

    public LocalTime getWorkEndTime() {
        return workEndTime;
    }

    public void setWorkEndTime(LocalTime workEndTime) {

        /*
         * Never allow the working end time to become null.
         */
        if (workEndTime == null) {
            this.workEndTime = LocalTime.of(18, 0);
        } else {
            this.workEndTime = workEndTime;
        }
    }


    /*
     * =========================================================
     * LATE GRACE MINUTES
     * =========================================================
     */

    public int getLateGraceMinutes() {
        return lateGraceMinutes;
    }

    public void setLateGraceMinutes(int lateGraceMinutes) {

        /*
         * Prevent negative grace periods.
         */
        if (lateGraceMinutes < 0) {
            this.lateGraceMinutes = 0;
        } else {
            this.lateGraceMinutes = lateGraceMinutes;
        }
    }


    /*
     * =========================================================
     * SUNDAY
     * =========================================================
     */

    public boolean isSunday() {
        return sunday;
    }

    public void setSunday(boolean sunday) {
        this.sunday = sunday;
    }


    /*
     * =========================================================
     * MONDAY
     * =========================================================
     */

    public boolean isMonday() {
        return monday;
    }

    public void setMonday(boolean monday) {
        this.monday = monday;
    }


    /*
     * =========================================================
     * TUESDAY
     * =========================================================
     */

    public boolean isTuesday() {
        return tuesday;
    }

    public void setTuesday(boolean tuesday) {
        this.tuesday = tuesday;
    }


    /*
     * =========================================================
     * WEDNESDAY
     * =========================================================
     */

    public boolean isWednesday() {
        return wednesday;
    }

    public void setWednesday(boolean wednesday) {
        this.wednesday = wednesday;
    }


    /*
     * =========================================================
     * THURSDAY
     * =========================================================
     */

    public boolean isThursday() {
        return thursday;
    }

    public void setThursday(boolean thursday) {
        this.thursday = thursday;
    }


    /*
     * =========================================================
     * FRIDAY
     * =========================================================
     */

    public boolean isFriday() {
        return friday;
    }

    public void setFriday(boolean friday) {
        this.friday = friday;
    }


    /*
     * =========================================================
     * SATURDAY
     * =========================================================
     */

    public boolean isSaturday() {
        return saturday;
    }

    public void setSaturday(boolean saturday) {
        this.saturday = saturday;
    }
}