# English Word App - Backend

English Word App（英语单词学习应用）后端服务

## 技术栈

- **Spring Boot**: 3.2.1
- **JDK**: 17
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA
- **WebSocket**: Spring WebSocket
- **AI服务**: 智谱AI (GLM-4)
- **API文档**: SpringDoc OpenAPI
- **构建工具**: Maven

## 项目结构

```
english-word-backend/
├── src/main/java/com/englishword/
│   ├── EnglishWordAppApplication.java    # 主启动类
│   ├── controller/                        # 控制器层
│   ├── service/                           # 服务层
│   ├── repository/                        # 数据访问层
│   ├── entity/                            # 实体类
│   ├── dto/                               # 数据传输对象
│   │   ├── request/                       # 请求DTO
│   │   └── response/                      # 响应DTO
│   ├── config/                            # 配置类
│   └── util/                              # 工具类
├── src/main/resources/
│   ├── application.yml                    # 应用配置
│   └── db/migration/                      # 数据库迁移脚本
├── pom.xml                                # Maven配置
└── README.md                              # 项目说明
```

## 快速开始

### 前置条件

1. JDK 17 或更高版本
2. Maven 3.6+
3. MySQL 8.0+
4. 智谱AI API Key（需要申请）

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE english_word_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改 `src/main/resources/application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/english_word_app?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

### 环境变量配置

设置智谱AI API Key环境变量：

**Windows:**
```powershell
set ZHIPUAI_API_KEY=your_api_key_here
```

**Linux/Mac:**
```bash
export ZHIPUAI_API_KEY=your_api_key_here
```

或者在 IDEA 中配置：
1. Run -> Edit Configurations
2. Environment variables 添加：`ZHIPUAI_API_KEY=your_api_key_here`

### 启动项目

**方式一：使用 Maven 启动**
```bash
cd english-word-backend
mvn clean install
mvn spring-boot:run
```

**方式二：使用 IDE 启动**
1. 使用 IntelliJ IDEA 打开项目
2. 找到 `EnglishWordAppApplication.java`
3. 右键 -> Run 'EnglishWordAppApplication'

**方式三：使用启动脚本（Windows）**
```bash
mvn clean package
java -jar target/english-word-backend-1.0.0.jar
```

### 访问服务

启动成功后，访问以下地址：

- **API文档**: http://localhost:8885/api/swagger-ui.html
- **API Docs (JSON)**: http://localhost:8885/api/api-docs
- **WebSocket**: ws://localhost:8885/api/ws

## API 端口

- **服务端口**: 8885
- **上下文路径**: /api

## 主要功能模块

### 1. 用户管理
- 用户注册
- 用户登录（JWT认证）
- 用户信息管理

### 2. 单词管理
- 单词查询
- 单词添加
- 单词收藏
- 生词本管理

### 3. 学习记录
- 学习进度跟踪
- 复习提醒
- 学习统计

### 4. AI 助手
- 单词讲解（发音、释义、例句）
- 词根词缀分析
- 同义词/反义词
- 记忆技巧

### 5. WebSocket
- 实时消息推送
- 在线学习提醒

## 配置说明

### JWT配置
```yaml
jwt:
  secret: EnglishWordAppSecretKey2026ForJWTTokenGenerationMustBeLongEnough
  expiration: 604800000  # 7天
```

### 智谱AI配置
```yaml
zhipuai:
  api-key: ${ZHIPUAI_API_KEY}  # 从环境变量读取
  api-url: https://open.bigmodel.cn/api/paas/v4/chat/completions
  model: glm-4-flash
```

## 开发指南

### 添加新的API端点

1. 在 `controller` 包下创建控制器类
2. 使用 `@RestController` 和 `@RequestMapping` 注解
3. 在 `service` 包下创建服务类
4. 在 `repository` 包下创建数据访问接口
5. 在 `entity` 包下创建实体类

### API文档注解

使用 SpringDoc 注解：
- `@Operation`: 描述API操作
- `@Parameter`: 描述参数
- `@ApiResponse`: 描述响应

## 数据库迁移

项目使用 Flyway 进行数据库版本管理：

1. 在 `src/main/resources/db/migration/` 目录下创建 SQL 脚本
2. 文件命名格式：`V{version}__{description}.sql`
3. 示例：`V1__init_schema.sql`

## 测试

运行单元测试：
```bash
mvn test
```

## 部署

### 构建生产包
```bash
mvn clean package -DskipTests
```

### 运行生产包
```bash
java -jar target/english-word-backend-1.0.0.jar
```

## 常见问题

### 1. 数据库连接失败
- 检查 MySQL 服务是否启动
- 确认端口号（默认3307）
- 验证用户名和密码

### 2. 智谱AI调用失败
- 确认 API Key 是否正确设置
- 检查网络连接
- 查看智谱AI服务状态

### 3. 端口冲突
- 修改 `application.yml` 中的 `server.port`
- 确保端口8885未被占用

## 技术支持

- **API文档**: http://localhost:8885/api/swagger-ui.html
- **GitHub Issues**: [提交问题](https://github.com/your-repo/issues)

## 许可证

MIT License

## 作者

English Word Team

---

**祝学习愉快！Happy Learning!**
