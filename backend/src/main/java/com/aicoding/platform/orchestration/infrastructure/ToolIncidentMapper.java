package com.aicoding.platform.orchestration.infrastructure;

import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ToolIncidentMapper extends BaseMapper<ToolIncidentEntity> {
}
