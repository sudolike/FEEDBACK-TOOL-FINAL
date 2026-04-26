package com.cen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RAG 知识库分片
 *  - 用于"基于本地数据库的检索增强生成"
 *  - 不依赖向量数据库，使用 MySQL 全文索引 (ngram) + 关键词匹配
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_kb_chunk")
public class KbChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** course / feedback / teacher / resource / qa */
    private String sourceType;

    private Long sourceId;
    private Long courseId;

    private String title;
    private String content;
    private String keywords;
    private Integer tokens;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
