package com.aicoding.platform.member.infrastructure;

import com.aicoding.platform.member.domain.ProjectMemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMemberEntity> {
}
