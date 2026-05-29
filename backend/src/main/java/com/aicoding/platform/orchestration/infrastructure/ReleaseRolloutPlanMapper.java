package com.aicoding.platform.orchestration.infrastructure;

import com.aicoding.platform.orchestration.domain.ReleaseRolloutPlanEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ReleaseRolloutPlanMapper extends BaseMapper<ReleaseRolloutPlanEntity> {
}
