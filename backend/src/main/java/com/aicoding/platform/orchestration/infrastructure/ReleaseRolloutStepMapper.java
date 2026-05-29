package com.aicoding.platform.orchestration.infrastructure;

import com.aicoding.platform.orchestration.domain.ReleaseRolloutStepEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ReleaseRolloutStepMapper extends BaseMapper<ReleaseRolloutStepEntity> {
}
