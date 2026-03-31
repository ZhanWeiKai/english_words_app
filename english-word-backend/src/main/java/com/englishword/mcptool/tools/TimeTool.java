package com.englishword.mcptool.tools;

import com.englishword.mcptool.annotation.McpParam;
import com.englishword.mcptool.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具类
 *
 * 提供时间相关的工具方法
 */
@Component
public class TimeTool {

    /**
     * 获取当前时间
     */
    @McpTool(name = "get_current_time", description = "获取当前日期和时间，返回格式化的时间字符串")
    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 获取当前时间戳
     */
    @McpTool(name = "get_timestamp", description = "获取当前 Unix 时间戳（毫秒）")
    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 格式化时间
     */
    @McpTool(name = "format_time", description = "将时间戳格式化为可读的时间字符串")
    public String formatTime(
            @McpParam(name = "timestamp", description = "Unix 时间戳（毫秒）") long timestamp,
            @McpParam(name = "pattern", description = "时间格式，如 yyyy-MM-dd HH:mm:ss", required = false) String pattern
    ) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        String format = (pattern != null && !pattern.isEmpty()) ? pattern : "yyyy-MM-dd HH:mm:ss";
        return dateTime.format(DateTimeFormatter.ofPattern(format));
    }
}
