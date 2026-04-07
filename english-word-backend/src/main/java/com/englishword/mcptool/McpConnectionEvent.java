package com.englishword.mcptool;

import org.springframework.context.ApplicationEvent;

/**
 * MCP 连接事件
 *
 * 用于在 McpToolServer 和 McpClient 之间协调重连
 */
public class McpConnectionEvent extends ApplicationEvent {

    /**
     * 事件类型
     */
    public enum Type {
        /**
         * 工具端已连接（工具已注册到 MCP Server）
         */
        TOOL_SERVER_CONNECTED,

        /**
         * 工具端已断开
         */
        TOOL_SERVER_DISCONNECTED
    }

    private final Type eventType;
    private final String message;

    public McpConnectionEvent(Object source, Type eventType, String message) {
        super(source);
        this.eventType = eventType;
        this.message = message;
    }

    public Type getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "McpConnectionEvent{" +
                "eventType=" + eventType +
                ", message='" + message + '\'' +
                ", source=" + source.getClass().getSimpleName() +
                '}';
    }
}
