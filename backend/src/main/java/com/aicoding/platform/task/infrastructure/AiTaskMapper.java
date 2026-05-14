package com.aicoding.platform.task.infrastructure;

import com.aicoding.platform.task.domain.AiTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiTaskMapper extends BaseMapper<AiTaskEntity> {
}
