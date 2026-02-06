package com.englishword;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * English Word App - 应用启动类
 *
 * @author English Word Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class EnglishWordAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishWordAppApplication.class, args);
        System.out.println("""

                ========================================
                   English Word App - 后端服务启动成功！
                   API文档: http://localhost:8885/api/swagger-ui.html
                   WebSocket: ws://localhost:8885/api/ws
                ========================================
                """);
    }

}
