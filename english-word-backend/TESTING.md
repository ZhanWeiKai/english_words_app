# English Word Backend - 单元测试说明

## 测试概述

本项目包含以下单元测试和集成测试：

### 测试类列表

1. **JwtUtilTest** - JWT工具类测试
   - 位置：`src/test/java/com/englishword/util/JwtUtilTest.java`
   - 测试内容：
     - Token生成
     - Token验证
     - 从Token中提取用户信息

2. **WordServiceTest** - 单词服务测试
   - 位置：`src/test/java/com/englishword/service/WordServiceTest.java`
   - 测试内容：
     - 添加单词
     - 获取单词详情
     - 更新单词信息
     - 删除单词
     - 更新掌握程度
     - 权限验证

3. **AuthServiceTest** - 认证服务测试
   - 位置：`src/test/java/com/englishword/service/AuthServiceTest.java`
   - 测试内容：
     - 用户注册
     - 用户登录
     - 用户名重复检查
     - 密码验证

4. **AuthControllerIntegrationTest** - 认证控制器集成测试
   - 位置：`src/test/java/com/englishword/controller/AuthControllerIntegrationTest.java`
   - 测试内容：
     - 注册+登录完整流程
     - 重复注册测试
     - 错误密码测试
     - Token验证测试

## 运行测试

### 运行所有测试
```bash
mvn test
```

### 运行特定测试类
```bash
mvn test -Dtest=JwtUtilTest
```

### 运行特定测试方法
```bash
mvn test -Dtest=JwtUtilTest#testGenerateToken
```

### 生成测试报告
```bash
mvn test jacoco:report
```

## 测试覆盖率

当前测试覆盖的模块：

✅ **已覆盖**
- JWT工具类
- 单词服务（CRUD操作）
- 认证服务（注册/登录）
- 认证控制器（集成测试）

⏳ **待补充**
- AI服务测试
- 单词控制器测试
- WebSocket处理器测试
- Repository层测试

## 测试数据

测试使用的数据：
- 测试用户名：`integration_test_user`
- 测试密码：`123456`
- 测试单词：`ephemeral`

所有测试数据会在测试结束后自动清理。

## 注意事项

1. **数据库依赖**：集成测试需要数据库连接
   - 确保MySQL服务正在运行
   - 端口：3307
   - 数据库：english_word_app

2. **环境变量**：AI服务测试需要设置环境变量
   ```bash
   export ZHIPUAI_API_KEY=your_test_api_key
   ```

3. **Mock使用**：单元测试使用Mockito框架
   - Mock外部依赖
   - 隔离测试逻辑

## 下一步

建议继续添加以下测试：
- [ ] WordController集成测试
- [ ] AIController集成测试
- - [ ] WebSocketHandler测试
- [ ] Repository层测试
- [ ] 端到端测试（E2E）

---

**测试框架**：JUnit 5 + Mockito + Spring Boot Test
