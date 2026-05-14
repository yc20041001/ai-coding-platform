package com.aicoding.platform.project.infrastructure;

import com.aicoding.platform.project.domain.ProjectConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectConfigMapper extends BaseMapper<ProjectConfigEntity> {
}
