package com.aicoding.platform.orchestrator.infrastructure;

import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelRequestLogMapper extends BaseMapper<ModelRequestLogEntity> {
}
