package com.englishword.mcptool;

import com.englishword.mcptool.annotation.McpTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * MCP 工具扫描器
 *
 * 自动扫描所有带有 @McpTool 注解的方法，并注册到 McpToolRegistry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolScanner implements BeanPostProcessor {

    private final McpToolRegistry toolRegistry;
    private final McpToolProperties properties;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!isToolServerEnabled()) {
            return bean;
        }

        String className = bean.getClass().getName();
        if (!isInScanPackages(className)) {
            return bean;
        }

        // 扫描所有方法
        Method[] methods = bean.getClass().getDeclaredMethods();
        for (Method method : methods) {
            McpTool mcpTool = method.getAnnotation(McpTool.class);
            if (mcpTool != null) {
                registerTool(bean, method, mcpTool);
            }
        }

        return bean;
    }

    private boolean isToolServerEnabled() {
        boolean enabled = properties.isEnabled() && properties.getScan() != null;

        if (enabled) {
            List<String> packages = properties.getScan().getBasePackages();
            log.debug("[MCP-Scanner] 工具扫描已启用，扫描包: {}", packages);
        }

        return enabled;
    }

    private boolean isInScanPackages(String className) {
        if (properties.getScan() == null) {
            return false;
        }

        List<String> basePackages = properties.getScan().getBasePackages();
        if (basePackages == null || basePackages.isEmpty()) {
            return false;
        }

        for (String basePackage : basePackages) {
            if (className.startsWith(basePackage)) {
                return true;
            }
        }

        return false;
    }

    private void registerTool(Object bean, Method method, McpTool mcpTool) {
        try {
            ToolDefinition toolDef = ToolDefinition.fromMethod(bean, method, mcpTool);
            toolRegistry.register(toolDef);
            log.info("[MCP-Scanner] 已扫描工具: {} -> {}.{}",
                    mcpTool.name(),
                    bean.getClass().getSimpleName(),
                    method.getName());
        } catch (Exception e) {
            log.error("[MCP-Scanner] 注册工具失败: {} - {}", mcpTool.name(), e.getMessage());
        }
    }
}
