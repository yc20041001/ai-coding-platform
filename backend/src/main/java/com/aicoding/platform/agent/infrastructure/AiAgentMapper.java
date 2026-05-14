package com.aicoding.platform.agent.infrastructure;

import com.aicoding.platform.agent.domain.AiAgentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiAgentMapper extends BaseMapper<AiAgentEntity> {
}
