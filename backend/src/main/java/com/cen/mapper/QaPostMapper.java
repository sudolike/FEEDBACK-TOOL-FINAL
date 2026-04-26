package com.cen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cen.entity.QaPost;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface QaPostMapper extends BaseMapper<QaPost> {
    @Update("UPDATE sys_qa_post SET view_count = view_count + 1 WHERE id = #{id}")
    int incrViewCount(@Param("id") Long id);

    @Update("UPDATE sys_qa_post SET reply_count = reply_count + 1 WHERE id = #{id}")
    int incrReplyCount(@Param("id") Long id);
}
