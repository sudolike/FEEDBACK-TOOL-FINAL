package com.cen.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 文件上传的列表
 * </p>
 *
 * @author wyt
 * @since 2025-02-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_file")
public class FileD implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 文件名称
    private String name;

    // 下载链接
    private String url;

    // 文件md5
    private String md5;

    // 文件类型
    private String type;

    // 文件大小
    private Long size;

    // 是否禁用(1-启用, 1-禁用)
    private Integer enable;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;

    // 是否删除(0-未删, 1-已删)
    private Integer isDeleted;
}
