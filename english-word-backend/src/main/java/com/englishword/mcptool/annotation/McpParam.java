package com.englishword.mcptool.annotation;

import java.lang.annotation.*;

/**
 * MCP 工具参数注解
 *
 * 用于描述工具方法的参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpParam {

    /**
     * 参数名称
     */
    String name();

    /**
     * 参数描述
     */
    String description();

    /**
     * 是否必填（默认 true）
     */
    boolean required() default true;
}
