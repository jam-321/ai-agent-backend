package com.jam.agent.monitoring.service;

import com.jam.agent.agent.service.AgentRunService;
import com.jam.agent.monitoring.dto.AdminConversationDetailResponse;
import com.jam.agent.monitoring.dto.AdminConversationPageResponse;
import com.jam.agent.monitoring.dto.AdminConversationSummaryResponse;
import com.jam.agent.monitoring.dto.AdminOverviewResponse;
import com.jam.agent.monitoring.dto.AdminToolStatisticsResponse;
import com.jam.agent.monitoring.persistence.repository.AdminMonitorRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminMonitorService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminMonitorRepository repository;

    public AdminMonitorService(AdminMonitorRepository repository) {
        this.repository = repository;
    }

    public AdminOverviewResponse overview() {
        return repository.overview();
    }

    public AdminConversationPageResponse conversations(
            int requestedPage,
            int requestedSize,
            String search) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE);
        int offset = (page - 1) * size;

        return new AdminConversationPageResponse(
                repository.conversations(search, offset, size),
                page,
                size,
                repository.countConversations(search));
    }

    public AdminConversationDetailResponse conversation(long conversationId) {
        AdminConversationSummaryResponse conversation = repository.conversation(conversationId)
                .orElseThrow(AgentRunService.NotFoundException::new);
        return new AdminConversationDetailResponse(
                conversation,
                repository.turns(conversationId),
                repository.nodes(conversationId));
    }

    public List<AdminToolStatisticsResponse> tools() {
        return repository.toolStatistics();
    }
}
