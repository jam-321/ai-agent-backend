package com.jam.agent.monitoring.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface AdminMonitorMapper {

    Map<String, Object> selectOverview();

    long countConversations(@Param("search") String search);

    List<Map<String, Object>> selectConversations(
            @Param("search") String search,
            @Param("offset") int offset,
            @Param("size") int size);

    Map<String, Object> selectConversation(@Param("conversationId") long conversationId);

    Long selectConversationIdByTrace(@Param("traceId") String traceId);

    List<Map<String, Object>> selectTurns(@Param("conversationId") long conversationId);

    List<Map<String, Object>> selectNodes(@Param("conversationId") long conversationId);

    List<Map<String, Object>> selectToolStatistics();
}
