package com.cen.service.impl;

import com.cen.entity.FileD;
import com.cen.mapper.FileMapper;
import com.cen.service.IFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, FileD> implements IFileService {

}
