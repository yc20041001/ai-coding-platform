package com.aicoding.platform.github.infrastructure;

import com.aicoding.platform.github.domain.GithubPullRequestCacheEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GithubPullRequestCacheMapper extends BaseMapper<GithubPullRequestCacheEntity> {
}
