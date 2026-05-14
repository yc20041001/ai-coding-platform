package com.aicoding.platform.agent.infrastructure;

import com.aicoding.platform.agent.domain.ProjectAgentConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectAgentConfigMapper extends BaseMapper<ProjectAgentConfigEntity> {
}
