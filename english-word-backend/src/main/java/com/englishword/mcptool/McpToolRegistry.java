package com.englishword.mcptool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具注册中心
 */
@Slf4j
@Component
public class McpToolRegistry {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    /**
     * 注册工具
     */
    public void register(ToolDefinition toolDefinition) {
        String name = toolDefinition.getName();
        if (tools.containsKey(name)) {
            log.warn("[MCP-Registry] 工具已存在，将被覆盖: {}", name);
        }
        tools.put(name, toolDefinition);
        log.info("[MCP-Registry] 已注册工具: {} - {}", name, toolDefinition.getDescription());
    }

    /**
     * 获取工具
     */
    public ToolDefinition getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * 获取所有工具（MCP tools/list 格式）
     */
    public JSONArray getToolsArray() {
        JSONArray toolsArray = new JSONArray();
        for (ToolDefinition tool : tools.values()) {
            toolsArray.add(tool.toMcpFormat());
        }
        return toolsArray;
    }

    /**
     * 调用工具
     */
    public InvokeResult invoke(String toolName, JSONObject arguments) {
        ToolDefinition tool = tools.get(toolName);
        if (tool == null) {
            return InvokeResult.error("Unknown tool: " + toolName);
        }

        try {
            Object[] args = buildArguments(tool, arguments);
            Object result = tool.getMethod().invoke(tool.getTarget(), args);

            if (result == null) {
                return InvokeResult.success("null");
            }
            return InvokeResult.success(String.valueOf(result));

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("[MCP-Registry] 工具调用失败: {} - {}", toolName, cause.getMessage());
            return InvokeResult.error("Tool invocation failed: " + cause.getMessage());
        }
    }

    private Object[] buildArguments(ToolDefinition tool, JSONObject arguments) {
        if (arguments == null) {
            arguments = new JSONObject();
        }

        int paramCount = tool.getParameterNames().size();
        Object[] args = new Object[paramCount];

        for (int i = 0; i < paramCount; i++) {
            String paramName = tool.getParameterNames().get(i);
            Class<?> paramType = tool.getParameterTypes().get(i);
            Object value = arguments.get(paramName);
            args[i] = convertValue(value, paramType);
        }

        return args;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return getDefaultValue(targetType);
        }
        if (targetType.isInstance(value)) {
            return value;
        }

        String strValue = String.valueOf(value);

        if (targetType == String.class) return strValue;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(strValue);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(strValue);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(strValue);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(strValue);

        return value;
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        return null;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class InvokeResult {
        private boolean success;
        private String content;
        private String error;

        public static InvokeResult success(String content) {
            return new InvokeResult(true, content, null);
        }

        public static InvokeResult error(String error) {
            return new InvokeResult(false, null, error);
        }
    }
}
