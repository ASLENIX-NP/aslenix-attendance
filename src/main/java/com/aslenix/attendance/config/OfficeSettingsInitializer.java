package com.aslenix.attendance.config;

import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
public class OfficeSettingsInitializer {

    @Bean
    CommandLineRunner initializeOfficeSettings(
            OfficeSettingsRepository repository) {

        return args -> {

            // Do not create duplicate office settings
            if (repository.count() > 0) {
                return;
            }

            OfficeSettings settings = new OfficeSettings();

            // ==============================
            // OFFICE INFORMATION
            // ==============================

            settings.setOfficeName("ASLENIX");

            settings.setOfficeLocation(
                    "Rapti Digital Photo Studio"
            );

            // ==============================
            // OFFICE GPS LOCATION
            // ==============================

            settings.setLatitude(27.687524);

            settings.setLongitude(85.330353);

            // Employees must be within
            // 100 meters of the office
            // to check in.
            settings.setAllowedRadiusMeters(100);

            // ==============================
            // WORKING HOURS
            // ==============================

            settings.setWorkStartTime(
                    LocalTime.of(10, 0)
            );

            settings.setWorkEndTime(
                    LocalTime.of(18, 0)
            );

            // Employee is considered late
            // after 10:26 AM.
            settings.setLateGraceMinutes(26);

            // ==============================
            // WORKING DAYS
            // ==============================

            settings.setSunday(true);
            settings.setMonday(true);
            settings.setTuesday(true);
            settings.setWednesday(true);
            settings.setThursday(true);
            settings.setFriday(true);

            // Saturday is off
            settings.setSaturday(false);

            // ==============================
            // SAVE
            // ==============================

            repository.save(settings);

            // ==============================
            // CONSOLE INFORMATION
            // ==============================

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "     ASLENIX OFFICE SETTINGS CREATED"
            );

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "Office       : " +
                    settings.getOfficeName()
            );

            System.out.println(
                    "Location     : " +
                    settings.getOfficeLocation()
            );

            System.out.println(
                    "Latitude     : " +
                    settings.getLatitude()
            );

            System.out.println(
                    "Longitude    : " +
                    settings.getLongitude()
            );

            System.out.println(
                    "Radius       : " +
                    settings.getAllowedRadiusMeters() +
                    " meters"
            );

            System.out.println(
                    "Working time : 10:00 AM - 6:00 PM"
            );

            System.out.println(
                    "Late after   : 10:26 AM"
            );

            System.out.println(
                    "Working days : Sunday - Friday"
            );

            System.out.println(
                    "Saturday     : OFF"
            );

            System.out.println(
                    "========================================"
            );
        };
    }
}