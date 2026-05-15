package com.aicoding.platform.github.infrastructure;

import com.aicoding.platform.github.domain.GithubRepositoryCacheEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GithubRepositoryCacheMapper extends BaseMapper<GithubRepositoryCacheEntity> {
}
