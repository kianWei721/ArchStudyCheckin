package com.archstudy.checkin.studyplan.controller;

import com.archstudy.checkin.common.BusinessException;
import com.archstudy.checkin.common.ErrorCode;
import com.archstudy.checkin.common.Result;
import com.archstudy.checkin.security.SecurityContext;
import com.archstudy.checkin.studyplan.dto.CreateStudyPlanRequest;
import com.archstudy.checkin.studyplan.dto.StudyPlanResponse;
import com.archstudy.checkin.studyplan.dto.UpdateStudyPlanRequest;
import com.archstudy.checkin.studyplan.service.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study-plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @PostMapping
    public Result<StudyPlanResponse> createStudyPlan(@Valid @RequestBody CreateStudyPlanRequest request) {
        Long userId = getCurrentUserId();
        StudyPlanResponse response = studyPlanService.createStudyPlan(userId, request);
        return Result.success(response);
    }

    @GetMapping
    public Result<List<StudyPlanResponse>> listStudyPlans() {
        Long userId = getCurrentUserId();
        List<StudyPlanResponse> response = studyPlanService.listStudyPlans(userId);
        return Result.success(response);
    }

    @GetMapping("/current")
    public Result<StudyPlanResponse> getCurrentStudyPlan() {
        Long userId = getCurrentUserId();
        StudyPlanResponse response = studyPlanService.getCurrentStudyPlan(userId);
        return Result.success(response);
    }

    @GetMapping("/{planId}")
    public Result<StudyPlanResponse> getStudyPlan(@PathVariable Long planId) {
        Long userId = getCurrentUserId();
        StudyPlanResponse response = studyPlanService.getStudyPlan(userId, planId);
        return Result.success(response);
    }

    @PutMapping("/{planId}")
    public Result<StudyPlanResponse> updateStudyPlan(@PathVariable Long planId,
                                                     @Valid @RequestBody UpdateStudyPlanRequest request) {
        Long userId = getCurrentUserId();
        StudyPlanResponse response = studyPlanService.updateStudyPlan(userId, planId, request);
        return Result.success(response);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return userId;
    }
}
