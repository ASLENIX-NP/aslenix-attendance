package com.aslenix.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.aslenix.attendance.repository")
public class AslenixAttendanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AslenixAttendanceApplication.class, args);
    }
}