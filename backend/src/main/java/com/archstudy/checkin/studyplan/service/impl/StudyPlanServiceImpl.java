package com.archstudy.checkin.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.archstudy.checkin.common.BusinessException;
import com.archstudy.checkin.common.ErrorCode;
import com.archstudy.checkin.studyplan.dto.CreateStudyPlanRequest;
import com.archstudy.checkin.studyplan.dto.StudyPlanResponse;
import com.archstudy.checkin.studyplan.dto.UpdateStudyPlanRequest;
import com.archstudy.checkin.studyplan.entity.StudyPlan;
import com.archstudy.checkin.studyplan.mapper.StudyPlanMapper;
import com.archstudy.checkin.studyplan.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPlanServiceImpl implements StudyPlanService {

    private final StudyPlanMapper studyPlanMapper;

    @Override
    @Transactional
    public StudyPlanResponse createStudyPlan(Long userId, CreateStudyPlanRequest request) {
        // Check if user has any existing plans
        Long existingCount = studyPlanMapper.selectCount(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
        );

        boolean shouldBeCurrent;
        if (existingCount == 0) {
            // First plan is always the current plan
            shouldBeCurrent = true;
        } else {
            shouldBeCurrent = Boolean.TRUE.equals(request.getIsCurrent());
        }

        // If this plan should be current, clear other plans' isCurrent
        if (shouldBeCurrent && existingCount > 0) {
            clearCurrentPlan(userId);
        }

        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setPlanName(request.getPlanName());
        plan.setExamName(request.getExamName());
        plan.setTargetExamDate(request.getTargetExamDate());
        plan.setDailyTargetMinutes(request.getDailyTargetMinutes());
        plan.setWeeklyTargetDays(request.getWeeklyTargetDays());
        plan.setStage(request.getStage());
        plan.setIsCurrent(shouldBeCurrent ? 1 : 0);
        plan.setStatus(1);
        plan.setDeleted(0);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());

        studyPlanMapper.insert(plan);

        return toResponse(plan);
    }

    @Override
    public List<StudyPlanResponse> listStudyPlans(Long userId) {
        List<StudyPlan> plans = studyPlanMapper.selectList(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .orderByDesc(StudyPlan::getCreatedAt)
        );
        return plans.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public StudyPlanResponse getCurrentStudyPlan(Long userId) {
        StudyPlan plan = studyPlanMapper.selectOne(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .eq(StudyPlan::getIsCurrent, 1)
                        .eq(StudyPlan::getStatus, 1)
        );
        if (plan == null) {
            return null;
        }
        return toResponse(plan);
    }

    @Override
    public StudyPlanResponse getStudyPlan(Long userId, Long planId) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该学习计划");
        }
        return toResponse(plan);
    }

    @Override
    @Transactional
    public StudyPlanResponse updateStudyPlan(Long userId, Long planId, UpdateStudyPlanRequest request) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改该学习计划");
        }

        // Handle isCurrent change
        if (Boolean.TRUE.equals(request.getIsCurrent()) && plan.getIsCurrent() != 1) {
            clearCurrentPlan(userId);
            plan.setIsCurrent(1);
        } else if (Boolean.FALSE.equals(request.getIsCurrent())) {
            plan.setIsCurrent(0);
        }

        if (request.getPlanName() != null) {
            plan.setPlanName(request.getPlanName());
        }
        if (request.getExamName() != null) {
            plan.setExamName(request.getExamName());
        }
        if (request.getTargetExamDate() != null) {
            plan.setTargetExamDate(request.getTargetExamDate());
        }
        if (request.getDailyTargetMinutes() != null) {
            plan.setDailyTargetMinutes(request.getDailyTargetMinutes());
        }
        if (request.getWeeklyTargetDays() != null) {
            plan.setWeeklyTargetDays(request.getWeeklyTargetDays());
        }
        if (request.getStage() != null) {
            plan.setStage(request.getStage());
        }
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }

        plan.setUpdatedAt(LocalDateTime.now());
        studyPlanMapper.updateById(plan);

        return toResponse(plan);
    }

    private void clearCurrentPlan(Long userId) {
        studyPlanMapper.update(null,
                new LambdaUpdateWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .eq(StudyPlan::getIsCurrent, 1)
                        .set(StudyPlan::getIsCurrent, 0)
                        .set(StudyPlan::getUpdatedAt, LocalDateTime.now())
        );
    }

    private StudyPlanResponse toResponse(StudyPlan plan) {
        StudyPlanResponse response = new StudyPlanResponse();
        response.setId(plan.getId());
        response.setUserId(plan.getUserId());
        response.setPlanName(plan.getPlanName());
        response.setExamName(plan.getExamName());
        response.setTargetExamDate(plan.getTargetExamDate());
        response.setDailyTargetMinutes(plan.getDailyTargetMinutes());
        response.setWeeklyTargetDays(plan.getWeeklyTargetDays());
        response.setStage(plan.getStage());
        response.setIsCurrent(plan.getIsCurrent() != null && plan.getIsCurrent() == 1);
        response.setStatus(plan.getStatus());
        response.setCreatedAt(plan.getCreatedAt());
        response.setUpdatedAt(plan.getUpdatedAt());
        return response;
    }
}
