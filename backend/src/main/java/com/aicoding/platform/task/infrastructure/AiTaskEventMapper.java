package com.aicoding.platform.task.infrastructure;

import com.aicoding.platform.task.domain.AiTaskEventEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiTaskEventMapper extends BaseMapper<AiTaskEventEntity> {
}
