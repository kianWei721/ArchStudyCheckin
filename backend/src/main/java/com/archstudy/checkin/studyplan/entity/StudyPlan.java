package com.archstudy.checkin.studyplan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_plan")
public class StudyPlan {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String planName;

    private String examName;

    private LocalDate targetExamDate;

    private Integer dailyTargetMinutes;

    private Integer weeklyTargetDays;

    private String stage;

    private Integer isCurrent;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
