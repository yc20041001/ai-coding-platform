package com.aicoding.platform.rag.infrastructure;

import com.aicoding.platform.rag.domain.DocumentChunkEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunkEntity> {
}
