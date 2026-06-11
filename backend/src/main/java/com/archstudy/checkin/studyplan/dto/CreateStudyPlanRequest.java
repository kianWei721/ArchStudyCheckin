package com.archstudy.checkin.studyplan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateStudyPlanRequest {

    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称不能超过100个字符")
    private String planName;

    @Size(max = 100, message = "考试名称不能超过100个字符")
    private String examName;

    private LocalDate targetExamDate;

    @Min(value = 1, message = "每日目标分钟数最少为1")
    @Max(value = 1440, message = "每日目标分钟数最多为1440")
    private Integer dailyTargetMinutes;

    @Min(value = 1, message = "每周目标天数最少为1")
    @Max(value = 7, message = "每周目标天数最多为7")
    private Integer weeklyTargetDays;

    @Size(max = 50, message = "阶段不能超过50个字符")
    private String stage;

    private Boolean isCurrent;
}
