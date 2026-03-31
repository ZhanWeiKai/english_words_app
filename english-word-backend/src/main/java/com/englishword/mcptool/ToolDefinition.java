package com.englishword.mcptool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.mcptool.annotation.McpParam;
import com.englishword.mcptool.annotation.McpTool;
import lombok.Data;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP 工具定义
 */
@Data
public class ToolDefinition {

    private String name;
    private String description;
    private JSONObject inputSchema;
    private Object target;
    private Method method;
    private List<String> parameterNames = new ArrayList<>();
    private List<Class<?>> parameterTypes = new ArrayList<>();

    /**
     * 转换为 MCP 协议格式
     */
    public JSONObject toMcpFormat() {
        JSONObject tool = new JSONObject();
        tool.put("name", this.name);
        tool.put("description", this.description);
        tool.put("inputSchema", this.inputSchema);
        return tool;
    }

    /**
     * 从方法和注解构建 ToolDefinition
     */
    public static ToolDefinition fromMethod(Object target, Method method, McpTool toolAnnotation) {
        ToolDefinition def = new ToolDefinition();
        def.setName(toolAnnotation.name());
        def.setDescription(toolAnnotation.description());
        def.setTarget(target);
        def.setMethod(method);

        // 构建 JSON Schema
        JSONObject schema = new JSONObject();
        schema.put("type", "object");

        JSONObject properties = new JSONObject();
        List<String> required = new ArrayList<>();

        Parameter[] parameters = method.getParameters();
        for (Parameter param : parameters) {
            String paramName = param.getName();
            Class<?> paramType = param.getType();

            McpParam mcpParam = param.getAnnotation(McpParam.class);

            if (mcpParam != null) {
                paramName = mcpParam.name();
                def.getParameterNames().add(mcpParam.name());

                JSONObject paramSchema = new JSONObject();
                paramSchema.put("type", mapJavaTypeToJsonType(paramType));
                paramSchema.put("description", mcpParam.description());

                properties.put(mcpParam.name(), paramSchema);

                if (mcpParam.required()) {
                    required.add(mcpParam.name());
                }
            } else {
                def.getParameterNames().add(paramName);

                JSONObject paramSchema = new JSONObject();
                paramSchema.put("type", mapJavaTypeToJsonType(paramType));
                paramSchema.put("description", paramName);

                properties.put(paramName, paramSchema);
                required.add(paramName);
            }

            def.getParameterTypes().add(paramType);
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        def.setInputSchema(schema);
        return def;
    }

    private static String mapJavaTypeToJsonType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class ||
            type == long.class || type == Long.class ||
            type == short.class || type == Short.class) return "integer";
        if (type == float.class || type == Float.class ||
            type == double.class || type == Double.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type.isArray() || java.util.Collection.class.isAssignableFrom(type)) return "array";
        return "object";
    }
}
