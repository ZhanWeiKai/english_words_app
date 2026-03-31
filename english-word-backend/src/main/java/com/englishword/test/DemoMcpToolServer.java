package com.englishword.test;

import com.alibaba.fastjson2.JSONObject;

import java.util.Scanner;

/**
 * MCP 工具端 Demo
 *
 * 启动一个 MCP 工具服务器，注册一些示例工具
 *
 * 运行顺序:
 * 1. 先启动 MCP Endpoint Server (python main.py)
 * 2. 运行此 DemoMcpToolServer (工具端)
 * 3. 运行 DemoMcpClient (客户端) 来调用工具
 */
public class DemoMcpToolServer {

    // MCP Endpoint Server 配置
    // 工具端使用 /mcp/ 端点
    private static final String MCP_SERVER_URL = "ws://192.168.1.68:8004/mcp_endpoint/mcp/";

    // Token (从 MCP Server 启动日志复制)
    private static final String MCP_TOKEN = "mvPd7HCTRs1CB7Gc/6M4AcrmtRVnQjEhKvDWbn8Nq6w=";

    public static void main(String[] args) {
        System.out.println("========== MCP Tool Server Demo ==========\n");

        // 1. 创建工具服务器
        McpToolServer toolServer = new McpToolServer(MCP_SERVER_URL, MCP_TOKEN);

        // 2. 注册工具
        registerTools(toolServer);

        // 3. 连接到 MCP Endpoint Server
        System.out.println("[1] 正在连接 MCP Endpoint Server...");
        System.out.println("    URL: " + MCP_SERVER_URL);
        toolServer.connect();

        if (!toolServer.awaitConnection(5)) {
            System.err.println("\n连接失败! 请检查:");
            System.err.println("  1. MCP Endpoint Server 是否已启动");
            System.err.println("  2. URL 和 Token 是否正确");
            return;
        }

        System.out.println("[1] 已连接到 MCP Endpoint Server!\n");
        System.out.println("========================================");
        System.out.println("  工具服务器已启动，等待客户端调用...");
        System.out.println("  输入 'quit' 或按 Ctrl+C 退出");
        System.out.println("========================================\n");

        // 4. 等待用户输入退出
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            if ("quit".equalsIgnoreCase(input.trim())) {
                break;
            }
        }

        // 5. 关闭连接
        System.out.println("\n[结束] 正在关闭...");
        toolServer.close();
        scanner.close();
        System.out.println("========== Tool Server 已停止 ==========");
    }

    /**
     * 注册示例工具
     */
    private static void registerTools(McpToolServer server) {
        System.out.println("[2] 注册工具...\n");

        // 1. 计算器工具
        JSONObject calcSchema = new JSONObject();
        calcSchema.put("type", "object");
        JSONObject calcProps = new JSONObject();
        JSONObject expressionProp = new JSONObject();
        expressionProp.put("type", "string");
        expressionProp.put("description", "要计算的数学表达式，如 '6 + 7' 或 '10 * 5'");
        calcProps.put("expression", expressionProp);
        calcSchema.put("properties", calcProps);
        calcSchema.put("required", new String[]{"expression"});

        server.registerTool("calculator", "计算数学表达式", calcSchema, args -> {
            String expression = args.getString("expression");
            return evaluateExpression(expression);
        });

        // 2. 天气查询工具 (模拟)
        JSONObject weatherSchema = new JSONObject();
        weatherSchema.put("type", "object");
        JSONObject weatherProps = new JSONObject();
        JSONObject cityProp = new JSONObject();
        cityProp.put("type", "string");
        cityProp.put("description", "城市名称");
        weatherProps.put("city", cityProp);
        weatherSchema.put("properties", weatherProps);
        weatherSchema.put("required", new String[]{"city"});

        server.registerTool("get_weather", "获取城市天气信息 (模拟数据)", weatherSchema, args -> {
            String city = args.getString("city");
            return getMockWeather(city);
        });

        // 3. 字符串反转工具
        JSONObject reverseSchema = new JSONObject();
        reverseSchema.put("type", "object");
        JSONObject reverseProps = new JSONObject();
        JSONObject textProp = new JSONObject();
        textProp.put("type", "string");
        textProp.put("description", "要反转的文本");
        reverseProps.put("text", textProp);
        reverseSchema.put("properties", reverseProps);
        reverseSchema.put("required", new String[]{"text"});

        server.registerTool("reverse_string", "反转字符串", reverseSchema, args -> {
            String text = args.getString("text");
            return new StringBuilder(text).reverse().toString();
        });

        // 4. 获取时间工具
        JSONObject timeSchema = new JSONObject();
        timeSchema.put("type", "object");
        timeSchema.put("properties", new JSONObject());

        server.registerTool("get_current_time", "获取当前时间", timeSchema, args -> {
            return java.time.LocalDateTime.now().toString();
        });

        System.out.println();
    }

    /**
     * 计算数学表达式 (简单实现)
     */
    private static String evaluateExpression(String expression) {
        try {
            // 移除空格
            expression = expression.replaceAll("\\s+", "");

            // 支持基本运算: +, -, *, /
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                if (parts.length == 2) {
                    double a = Double.parseDouble(parts[0]);
                    double b = Double.parseDouble(parts[1]);
                    return String.valueOf(a + b);
                }
            } else if (expression.contains("-")) {
                String[] parts = expression.split("-");
                if (parts.length == 2) {
                    double a = Double.parseDouble(parts[0]);
                    double b = Double.parseDouble(parts[1]);
                    return String.valueOf(a - b);
                }
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                if (parts.length == 2) {
                    double a = Double.parseDouble(parts[0]);
                    double b = Double.parseDouble(parts[1]);
                    return String.valueOf(a * b);
                }
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                if (parts.length == 2) {
                    double a = Double.parseDouble(parts[0]);
                    double b = Double.parseDouble(parts[1]);
                    if (b == 0) {
                        return "Error: Division by zero";
                    }
                    return String.valueOf(a / b);
                }
            }

            // 尝试直接解析数字
            return String.valueOf(Double.parseDouble(expression));

        } catch (Exception e) {
            return "Error: 无法解析表达式 '" + expression + "'";
        }
    }

    /**
     * 获取模拟天气数据
     */
    private static String getMockWeather(String city) {
        // 模拟数据
        double temperature = 15 + Math.random() * 20;
        String[] conditions = {"晴天", "多云", "阴天", "小雨"};
        String condition = conditions[(int) (Math.random() * conditions.length)];
        int humidity = 40 + (int) (Math.random() * 40);

        return String.format("城市: %s\n天气: %s\n温度: %.1f°C\n湿度: %d%%",
                city, condition, temperature, humidity);
    }
}
