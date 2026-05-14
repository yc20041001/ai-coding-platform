package com.aicoding.platform.repository.infrastructure;

import com.aicoding.platform.repository.domain.ProjectRepositoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectRepositoryMapper extends BaseMapper<ProjectRepositoryEntity> {
}
