package com.englishword.mcptool.tools;

import com.alibaba.fastjson2.JSONObject;
import com.englishword.mcptool.annotation.McpParam;
import com.englishword.mcptool.annotation.McpTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 系统信息工具类
 *
 * 提供系统状态和环境信息
 */
@Slf4j
@Component
public class SystemInfoTool {

    @Value("${spring.application.name:english-word-backend}")
    private String applicationName;

    /**
     * 获取系统信息
     */
    @McpTool(name = "get_system_info", description = "获取当前系统的基本信息，包括应用名称、版本等")
    public String getSystemInfo() {
        JSONObject result = new JSONObject();
        result.put("applicationName", applicationName);
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("osName", System.getProperty("os.name"));
        result.put("osVersion", System.getProperty("os.version"));
        result.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        result.put("maxMemory", Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        result.put("freeMemory", Runtime.getRuntime().freeMemory() / 1024 / 1024 + " MB");

        return result.toJSONString();
    }

    /**
     * 健康检查
     */
    @McpTool(name = "health_check", description = "检查服务健康状态，返回OK表示服务正常")
    public String healthCheck() {
        JSONObject result = new JSONObject();
        result.put("status", "OK");
        result.put("timestamp", System.currentTimeMillis());
        result.put("message", "Service is running normally");

        return result.toJSONString();
    }

    /**
     * 计算器
     */
    @McpTool(name = "calculate", description = "执行简单的数学计算")
    public String calculate(
            @McpParam(name = "expression", description = "数学表达式，如 '2+3' 或 '10*5'") String expression
    ) {
        try {
            // 简单的数学计算支持
            expression = expression.replaceAll("\\s+", ""); // 移除空格

            double result;
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                result = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
            } else if (expression.contains("-") && expression.indexOf("-") > 0) {
                String[] parts = expression.split("(?<!^)-");
                result = Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                result = Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                result = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            } else {
                result = Double.parseDouble(expression);
            }

            JSONObject response = new JSONObject();
            response.put("expression", expression);
            response.put("result", result);
            return response.toJSONString();

        } catch (Exception e) {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid expression: " + expression);
            response.put("message", e.getMessage());
            return response.toJSONString();
        }
    }
}
