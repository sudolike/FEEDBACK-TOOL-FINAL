package com.cen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cen.entity.AssignmentSubmission;

import java.util.List;

public interface IAssignmentSubmissionService extends IService<AssignmentSubmission> {
    List<AssignmentSubmission> listByAssignment(Long assignmentId);
    AssignmentSubmission getOne(Long assignmentId, Long studentId);
}
