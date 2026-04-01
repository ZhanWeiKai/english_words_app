package com.englishword.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文 - 存储当前登录用户信息
 *
 * 由于 MCP 工具调用通过 WebSocket 跨线程执行，ThreadLocal 无法传递用户信息。
 * 因此使用"当前操作用户"模式：在发起 AI 请求前设置用户，MCP 工具执行时获取。
 *
 * 注意：这不是 ThreadLocal，而是全局变量，适用于单用户操作场景。
 * 在多用户并发场景下，需要改用参数传递方式。
 */
@Slf4j
public class UserContext {

    // 当前操作用户（用于 MCP 工具跨线程调用）
    private static String currentUserId = null;
    private static String currentUsername = null;

    // ThreadLocal 用于 HTTP 请求范围内的用户追踪
    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();

    /**
     * 设置当前用户（HTTP 请求范围）
     */
    public static void setCurrentUser(String userId, String username) {
        userIdHolder.set(userId);
        usernameHolder.set(username);
        log.debug("[UserContext] 设置用户(ThreadLocal): userId={}, username={}", userId, username);
    }

    /**
     * 获取当前用户ID（优先从 ThreadLocal 获取，否则从全局获取）
     */
    public static String getUserId() {
        // 优先从 ThreadLocal 获取
        String userId = userIdHolder.get();
        if (userId != null) {
            return userId;
        }
        // 如果 ThreadLocal 没有，从全局获取（MCP 工具调用场景）
        userId = currentUserId;
        if (userId == null) {
            log.warn("[UserContext] 未找到当前用户ID");
        }
        return userId;
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        String username = usernameHolder.get();
        if (username != null) {
            return username;
        }
        return currentUsername;
    }

    /**
     * 检查是否有当前用户
     */
    public static boolean hasUser() {
        return userIdHolder.get() != null || currentUserId != null;
    }

    /**
     * 清除当前用户（请求结束时调用）
     */
    public static void clear() {
        userIdHolder.remove();
        usernameHolder.remove();
        log.debug("[UserContext] 已清除 ThreadLocal 用户上下文");
    }

    // ==================== MCP 工具跨线程支持 ====================

    /**
     * 设置当前操作用户（用于 MCP 工具调用）
     *
     * 由于 MCP 工具通过 WebSocket 在不同线程执行，ThreadLocal 无法传递。
     * 此方法设置全局用户上下文，供 MCP 工具使用。
     *
     * 注意：在并发场景下可能有问题，但目前单用户操作是安全的。
     */
    public static void setCurrentOperationUser(String userId, String username) {
        currentUserId = userId;
        currentUsername = username;
        log.debug("[UserContext] 设置操作用户(全局): userId={}, username={}", userId, username);
    }

    /**
     * 清除当前操作用户（AI 请求完成后调用）
     */
    public static void clearOperationUser() {
        currentUserId = null;
        currentUsername = null;
        log.debug("[UserContext] 已清除全局操作用户");
    }

    /**
     * 获取当前操作用户ID（MCP 工具专用）
     */
    public static String getCurrentOperationUserId() {
        return currentUserId;
    }
}
