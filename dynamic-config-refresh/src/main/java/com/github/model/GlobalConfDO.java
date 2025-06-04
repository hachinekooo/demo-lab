package com.github.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("global_conf")
public class GlobalConfDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conf_key")
    private String confKey;

    @TableField("conf_value")
    private String confValue;

    @TableField("conf_group")
    private String confGroup;

    @TableField("comment")
    private String comment;

    @TableField("deleted")
    private Integer deleted;
}