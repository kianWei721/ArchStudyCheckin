package com.archstudy.checkin.studyplan.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudyPlanResponse {

    private Long id;

    private Long userId;

    private String planName;

    private String examName;

    private LocalDate targetExamDate;

    private Integer dailyTargetMinutes;

    private Integer weeklyTargetDays;

    private String stage;

    private Boolean isCurrent;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
