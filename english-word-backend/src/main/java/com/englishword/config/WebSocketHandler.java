package com.englishword.config;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket处理器
 *
 * 功能：
 * - 处理WebSocket连接
 * - 接收客户端消息
 * - 调用AI服务获取回复
 * - 推送AI回复到客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 存储所有活跃的WebSocket会话
     * Key: sessionId, Value: WebSocketSession
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 连接建立
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        log.info("WebSocket连接建立: sessionId={}, 当前连接数={}", sessionId, sessions.size());

        // 发送欢迎消息
        Map<String, Object> welcomeMessage = Map.of(
                "type", "connected",
                "message", "WebSocket连接成功",
                "sessionId", sessionId
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcomeMessage)));
    }

    /**
     * 接收消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();

        log.info("收到WebSocket消息: sessionId={}, message={}", sessionId, payload);

        try {
            // 解析消息
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String type = (String) messageData.get("type");

            if ("chat".equals(type)) {
                // 处理聊天消息
                handleChatMessage(session, messageData);
            } else if ("ping".equals(type)) {
                // 心跳检测
                Map<String, Object> pongMessage = Map.of("type", "pong");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pongMessage)));
            } else {
                // 未知消息类型
                Map<String, Object> errorMessage = Map.of(
                        "type", "error",
                        "message", "未知消息类型: " + type
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
            }

        } catch (Exception e) {
            log.error("处理WebSocket消息失败: sessionId={}", sessionId, e);

            Map<String, Object> errorMessage = Map.of(
                    "type", "error",
                    "message", "消息处理失败: " + e.getMessage()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
        }
    }

    /**
     * 连接关闭
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);

        log.info("WebSocket连接关闭: sessionId={}, status={}, 剩余连接数={}",
                sessionId, status, sessions.size());
    }

    /**
     * 连接异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket传输异常: sessionId={}", sessionId, exception);

        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(sessionId);
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(WebSocketSession session, Map<String, Object> messageData) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String userMessage = (String) messageData.get("message");
        String conversationId = (String) messageData.get("conversationId");

        // TODO: 这里应该调用AIConversationService获取AI回复
        // 为了简化，暂时返回一个模拟回复

        Map<String, Object> response = Map.of(
                "type", "ai_response",
                "message", "这是一个模拟的AI回复。实际使用时，请调用AIConversationService。",
                "conversationId", conversationId != null ? conversationId : "new"
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }

    /**
     * 向指定会话发送消息
     */
    public void sendMessageToSession(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("发送消息失败: sessionId={}", sessionId, e);
            }
        }
    }

    /**
     * 广播消息到所有连接
     */
    public void broadcast(String message) {
        sessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    log.error("广播消息失败: sessionId={}", sessionId, e);
                }
            }
        });
    }
}
