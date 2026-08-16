package com.jam.agent.service;

import com.jam.agent.config.AgentProperties;
import com.jam.agent.dto.ProgressNodeResponse;
import com.jam.agent.dto.ProgressResponse;
import com.jam.agent.repository.ConversationNodeRepository;
import com.jam.agent.repository.ConversationRepository;
import com.jam.agent.repository.ConversationTurnRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProgressQueryService {
    private final ConversationRepository conversations; private final ConversationTurnRepository turns; private final ConversationNodeRepository nodes; private final AgentProperties properties;
    public ProgressQueryService(ConversationRepository conversations, ConversationTurnRepository turns, ConversationNodeRepository nodes, AgentProperties properties) { this.conversations=conversations;this.turns=turns;this.nodes=nodes;this.properties=properties; }
    public ProgressResponse get(long userId,long conversationId,int turnId) {
        if(conversations.findForUser(userId,conversationId).isEmpty()) throw new AgentRunService.NotFoundException();
        var user=turns.findForUser(userId,conversationId,turnId,"user").orElseThrow(AgentRunService.NotFoundException::new);
        var assistant=turns.findForUser(userId,conversationId,turnId,"assistant").orElse(null);
        List<ConversationNodeRepository.NodeRecord> records=nodes.findByTurn(userId,conversationId,turnId);
        boolean generated=records.stream().anyMatch(n->n.type().equals("GENERATE"));
        boolean success=records.stream().anyMatch(n->n.type().equals("GENERATE") && n.status().equals("COMPLETE"));
        boolean error=records.stream().anyMatch(n->n.type().equals("GENERATE") && n.status().equals("ERROR")) || (assistant!=null && assistant.errorMessage()!=null);
        String status=success?"COMPLETE":(error?"ERROR":"REASONING");
        String answer=success && assistant!=null?assistant.content():null;
        String errorMessage=error && assistant!=null?assistant.errorMessage():null;
        return new ProgressResponse(conversationId,turnId,user.traceId(),status,answer,errorMessage,aggregate(records));
    }
    private List<ProgressNodeResponse> aggregate(List<ConversationNodeRepository.NodeRecord> records) {
        Map<String,List<ConversationNodeRepository.NodeRecord>> grouped=new LinkedHashMap<>();
        records.forEach(n->grouped.computeIfAbsent(n.aggrKey()==null?"#single-"+n.id():n.aggrKey(), k->new ArrayList<>()).add(n));
        return grouped.values().stream().sorted(Comparator.comparingLong(v->v.get(0).id())).map(this::toResponse).toList();
    }
    private ProgressNodeResponse toResponse(List<ConversationNodeRepository.NodeRecord> list) {
        var first=list.get(0); boolean multiple=first.aggrKey()!=null;
        if(!multiple) return node(first,"single",null,List.of());
        var terminal=list.stream().filter(n->n.status().equals("ERROR")||n.status().equals("SUCCESS")).max(Comparator.comparingLong(ConversationNodeRepository.NodeRecord::id)).orElse(null);
        String status=list.stream().anyMatch(n->n.status().equals("ERROR"))?"ERROR":(terminal==null?"START":"SUCCESS");
        return new ProgressNodeResponse("multiple",first.id(),first.aggrKey(),first.nodeId(),first.nodeName(),first.type(),status,null,false,
                first.createdAt(),terminal==null?first.updatedAt():terminal.updatedAt(),list.stream().map(n->node(n,"single",null,List.of())).toList());
    }
    private ProgressNodeResponse node(ConversationNodeRepository.NodeRecord n,String structure,String aggr,List<ProgressNodeResponse> children) {
        int limit=n.status().equals("START")?properties.getProgress().getMaxToolArgsPreviewChars():properties.getProgress().getMaxToolResultPreviewChars();
        String content=n.content(); boolean truncated=content!=null && content.length()>limit; if(truncated) content=content.substring(0,limit);
        return new ProgressNodeResponse(structure,n.id(),aggr,n.nodeId(),n.nodeName(),n.type(),n.status(),content,truncated,n.createdAt(),n.updatedAt(),children);
    }
}
