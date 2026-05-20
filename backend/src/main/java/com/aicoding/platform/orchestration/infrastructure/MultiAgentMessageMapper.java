package com.aicoding.platform.orchestration.infrastructure;

import com.aicoding.platform.orchestration.domain.MultiAgentMessageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MultiAgentMessageMapper extends BaseMapper<MultiAgentMessageEntity> {
}
