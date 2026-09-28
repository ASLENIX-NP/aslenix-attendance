package com.aslenix.attendance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "abbreviation", length = 20)
    private String abbreviation;

    @Column(nullable = false)
    private boolean active = true;

    public Department() {
    }

    public Department(String name) {
        this.name = name;
    }

    public Department(String name, String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getEffectiveAbbreviation() {
        if (abbreviation != null && !abbreviation.trim().isEmpty()) {
            return abbreviation.trim().toUpperCase();
        }
        if (name != null) {
            String n = name.trim().toLowerCase();
            if (n.contains("front") || n.contains("ui") || n.contains("ux")) {
                return "FE";
            } else if (n.contains("back")) {
                return "BE";
            } else if (n.contains("admin")) {
                return "AD";
            } else if (n.contains("market") || n.contains("digital")) {
                return "DM";
            }
            String cleaned = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (cleaned.length() >= 2) {
                return cleaned.substring(0, 2);
            }
            return cleaned.isEmpty() ? "EMP" : cleaned;
        }
        return "EMP";
    }
}