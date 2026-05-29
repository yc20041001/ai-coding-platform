package com.aicoding.platform.orchestration.infrastructure;

import com.aicoding.platform.orchestration.domain.ReleaseVerificationRecordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ReleaseVerificationRecordMapper extends BaseMapper<ReleaseVerificationRecordEntity> {
}
