package com.cen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cen.entity.AssignmentSubmission;
import com.cen.mapper.AssignmentSubmissionMapper;
import com.cen.service.IAssignmentSubmissionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssignmentSubmissionServiceImpl extends ServiceImpl<AssignmentSubmissionMapper, AssignmentSubmission> implements IAssignmentSubmissionService {

    @Override
    public List<AssignmentSubmission> listByAssignment(Long assignmentId) {
        QueryWrapper<AssignmentSubmission> qw = new QueryWrapper<>();
        qw.eq("assignment_id", assignmentId).orderByDesc("submitted_at");
        return list(qw);
    }

    @Override
    public AssignmentSubmission getOne(Long assignmentId, Long studentId) {
        QueryWrapper<AssignmentSubmission> qw = new QueryWrapper<>();
        qw.eq("assignment_id", assignmentId).eq("student_id", studentId);
        return getOne(qw);
    }
}
