package com.englishword.mcptool.annotation;

import java.lang.annotation.*;

/**
 * MCP 工具注解
 *
 * 标记一个方法为 MCP 工具，AI 可以通过 MCP 协议调用此方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpTool {

    /**
     * 工具名称（建议使用 snake_case 格式）
     */
    String name();

    /**
     * 工具描述（AI 会看到这个描述来决定是否调用）
     */
    String description();
}
