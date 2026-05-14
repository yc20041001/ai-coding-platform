package com.aicoding.platform.chat.infrastructure;

import com.aicoding.platform.chat.domain.ChatMessageReferenceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageReferenceMapper extends BaseMapper<ChatMessageReferenceEntity> {
}
