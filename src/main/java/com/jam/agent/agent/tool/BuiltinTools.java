package com.jam.agent.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.repository.ConversationNodeRepository;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BuiltinTools {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final ConversationNodeRepository nodes;
    private final ObjectMapper objectMapper;

    public BuiltinTools(ConversationNodeRepository nodes, ObjectMapper objectMapper) {
        this.nodes = nodes; this.objectMapper = objectMapper;
    }

    @Tool(name = "current_time", description = "返回当前日期、时间、星期和 Asia/Shanghai 时区。")
    public String currentTime(ToolContext toolContext) throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        return objectMapper.writeValueAsString(Map.of(
                "isoTime", ISO.format(now), "displayTime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "weekday", now.getDayOfWeek().toString(), "weekdayZh", weekdayZh(now.getDayOfWeek().getValue()), "timezone", ZONE.getId()));
    }

    @Tool(name = "calculate", description = "使用精确十进制定点数计算基本算术表达式，支持 + - * / 和括号。")
    public String calculate(@ToolParam(description = "算术表达式，例如 345678*912345") String expression,
                            ToolContext toolContext) throws Exception {
        if (expression == null || expression.length() > 300) throw new IllegalArgumentException("表达式为空或过长。");
        BigDecimal result = new DecimalExpressionParser(expression).parse();
        return objectMapper.writeValueAsString(Map.of("expression", expression, "result", result.stripTrailingZeros().toPlainString()));
    }

    @Tool(name = "query_conversation_node", description = "查询当前会话历史某轮工具调用的完整入参和结果。只能查询当前会话且目标轮次必须早于当前轮次。")
    public String queryConversationNode(@ToolParam(description = "目标历史轮次") Integer targetTurnId,
                                         @ToolParam(description = "工具调用 ID") String aggrKey,
                                         ToolContext toolContext) throws Exception {
        AgentExecutionContext context = context(toolContext);
        if (targetTurnId == null || targetTurnId >= context.turnId() || targetTurnId < 1 || aggrKey == null || aggrKey.isBlank()) {
            throw new IllegalArgumentException("只能查询当前会话中更早轮次的有效工具调用。");
        }
        List<ConversationNodeRepository.NodeRecord> records = nodes.findToolNodes(context.userId(), context.conversationId(), targetTurnId, aggrKey);
        if (records.isEmpty()) throw new IllegalArgumentException("未找到对应的历史工具调用。");
        return objectMapper.writeValueAsString(records.stream().map(n -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("status", n.status()); value.put("tool", n.nodeName()); value.put("content", n.content());
            return value;
        }).toList());
    }

    private AgentExecutionContext context(ToolContext toolContext) {
        Object value = toolContext == null ? null : toolContext.getContext().get("executionContext");
        if (!(value instanceof AgentExecutionContext context)) throw new IllegalArgumentException("工具上下文无效。");
        return context;
    }
    private String weekdayZh(int day) { return List.of("", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日").get(day); }

    static final class DecimalExpressionParser {
        private final String input; private int index;
        DecimalExpressionParser(String input) { this.input = input; }
        BigDecimal parse() {
            BigDecimal result = expression(); skip();
            if (index != input.length()) throw new IllegalArgumentException("表达式包含不支持的字符。");
            return result;
        }
        private BigDecimal expression() { BigDecimal value = term(); while (true) { skip(); if (take('+')) value=value.add(term()); else if (take('-')) value=value.subtract(term()); else return value; } }
        private BigDecimal term() { BigDecimal value = factor(); while (true) { skip(); if (take('*')) value=value.multiply(factor()); else if (take('/')) { BigDecimal divisor=factor(); if (divisor.signum()==0) throw new IllegalArgumentException("不能除以零。"); value=value.divide(divisor, 32, java.math.RoundingMode.HALF_UP).stripTrailingZeros(); } else return value; } }
        private BigDecimal factor() { skip(); if (take('+')) return factor(); if (take('-')) return factor().negate(); if (take('(')) { BigDecimal v=expression(); if(!take(')')) throw new IllegalArgumentException("括号不匹配。"); return v; } int start=index; while(index<input.length() && (Character.isDigit(input.charAt(index)) || input.charAt(index)=='.')) index++; if(start==index) throw new IllegalArgumentException("需要数字。"); try { return new BigDecimal(input.substring(start,index)); } catch(NumberFormatException ex) { throw new IllegalArgumentException("数字格式无效。"); } }
        private boolean take(char c) { if(index<input.length() && input.charAt(index)==c) { index++; return true; } return false; }
        private void skip() { while(index<input.length() && Character.isWhitespace(input.charAt(index))) index++; }
    }
}
