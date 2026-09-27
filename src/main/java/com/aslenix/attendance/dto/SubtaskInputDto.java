package com.aslenix.attendance.dto;

public class SubtaskInputDto {

    private Integer weekNumber;
    private String title;
    private String description;
    private Long assigneeId;

    public SubtaskInputDto() {
    }

    public SubtaskInputDto(Integer weekNumber, String title, String description, Long assigneeId) {
        this.weekNumber = weekNumber;
        this.title = title;
        this.description = description;
        this.assigneeId = assigneeId;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
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

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}
