package com.aicoding.platform.repository.infrastructure;

import com.aicoding.platform.repository.domain.RepositoryBranchEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RepositoryBranchMapper extends BaseMapper<RepositoryBranchEntity> {
}
