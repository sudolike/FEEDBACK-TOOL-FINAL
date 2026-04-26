package com.cen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cen.entity.Assignment;

import java.util.List;

public interface IAssignmentService extends IService<Assignment> {
    List<Assignment> listByCourse(Long courseId);
    List<Assignment> listByStudent(Long studentId);
}
