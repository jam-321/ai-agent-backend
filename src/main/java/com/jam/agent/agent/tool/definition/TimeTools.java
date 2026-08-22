package com.jam.agent.agent.tool.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class TimeTools implements AgentToolProvider {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> WEEKDAYS = List.of(
            "", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日");

    private final ObjectMapper objectMapper;

    public TimeTools(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Tool(name = "current_time", description = "返回当前日期、时间、星期和 Asia/Shanghai 时区。")
    public String currentTime(ToolContext toolContext) throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        return objectMapper.writeValueAsString(Map.of(
                "isoTime", ISO.format(now),
                "displayTime", now.format(DISPLAY),
                "weekday", now.getDayOfWeek().toString(),
                "weekdayZh", WEEKDAYS.get(now.getDayOfWeek().getValue()),
                "timezone", ZONE.getId()));
    }
}
