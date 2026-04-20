package com.cen.service.impl;

import com.cen.entity.Responses;
import com.cen.mapper.ResponsesMapper;
import com.cen.service.IResponsesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class ResponsesServiceImpl extends ServiceImpl<ResponsesMapper, Responses> implements IResponsesService {

}
