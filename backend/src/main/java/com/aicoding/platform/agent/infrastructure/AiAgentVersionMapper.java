package com.aicoding.platform.agent.infrastructure;

import com.aicoding.platform.agent.domain.AiAgentVersionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiAgentVersionMapper extends BaseMapper<AiAgentVersionEntity> {
}
