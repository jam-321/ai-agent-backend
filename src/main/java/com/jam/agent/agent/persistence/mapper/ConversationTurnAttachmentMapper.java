package com.jam.agent.agent.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.agent.persistence.entity.ConversationTurnAttachmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ConversationTurnAttachmentMapper extends BaseMapper<ConversationTurnAttachmentEntity> {
    List<ConversationTurnAttachmentEntity> selectForTurn(
            @Param("userId") long userId, @Param("conversationId") long conversationId, @Param("turnId") int turnId);
    List<ConversationTurnAttachmentEntity> selectHistory(
            @Param("userId") long userId, @Param("conversationId") long conversationId, @Param("currentTurnId") int currentTurnId);
}
