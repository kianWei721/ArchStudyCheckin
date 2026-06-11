package com.archstudy.checkin.studyplan.service;

import com.archstudy.checkin.studyplan.dto.CreateStudyPlanRequest;
import com.archstudy.checkin.studyplan.dto.StudyPlanResponse;
import com.archstudy.checkin.studyplan.dto.UpdateStudyPlanRequest;

import java.util.List;

public interface StudyPlanService {

    StudyPlanResponse createStudyPlan(Long userId, CreateStudyPlanRequest request);

    List<StudyPlanResponse> listStudyPlans(Long userId);

    StudyPlanResponse getCurrentStudyPlan(Long userId);

    StudyPlanResponse getStudyPlan(Long userId, Long planId);

    StudyPlanResponse updateStudyPlan(Long userId, Long planId, UpdateStudyPlanRequest request);
}
