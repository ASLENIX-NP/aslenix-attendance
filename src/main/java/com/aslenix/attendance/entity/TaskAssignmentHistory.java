package com.aslenix.attendance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_assignment_histories")
public class TaskAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    @JsonIgnore
    private TaskAssignment assignment;

    @Column(name = "previous_progress", nullable = false)
    private Integer previousProgress = 0;

    @Column(name = "new_progress", nullable = false)
    private Integer newProgress = 0;

    @Column(name = "note", nullable = false, columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private Employee updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TaskAssignmentHistory() {
        this.createdAt = LocalDateTime.now();
    }

    public TaskAssignmentHistory(TaskAssignment assignment, Integer previousProgress, Integer newProgress, String note, Employee updatedBy) {
        this.assignment = assignment;
        this.previousProgress = previousProgress != null ? previousProgress : 0;
        this.newProgress = newProgress != null ? newProgress : 0;
        this.note = note != null ? note.trim() : "";
        this.updatedBy = updatedBy;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public TaskAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(TaskAssignment assignment) {
        this.assignment = assignment;
    }

    public Integer getPreviousProgress() {
        return previousProgress;
    }

    public void setPreviousProgress(Integer previousProgress) {
        this.previousProgress = previousProgress;
    }

    public Integer getNewProgress() {
        return newProgress;
    }

    public void setNewProgress(Integer newProgress) {
        this.newProgress = newProgress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Employee getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Employee updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
