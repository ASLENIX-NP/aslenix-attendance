package com.aslenix.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee_sequences")
public class EmployeeSequence {

    @Id
    @Column(name = "sequence_name", length = 50)
    private String sequenceName;

    @Column(name = "next_number", nullable = false)
    private Long nextNumber;

    public EmployeeSequence() {
    }

    public EmployeeSequence(String sequenceName, Long nextNumber) {
        this.sequenceName = sequenceName;
        this.nextNumber = nextNumber;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public Long getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(Long nextNumber) {
        this.nextNumber = nextNumber;
    }
}
