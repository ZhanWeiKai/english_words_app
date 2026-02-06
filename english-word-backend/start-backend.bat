@echo off
chcp 65001 > nul
echo ========================================
echo    English Word App - Backend 启动脚本
echo ========================================
echo.

REM 检查 Maven 是否安装
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Maven，请先安装 Maven
    pause
    exit /b 1
)

REM 检查 Java 是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Java，请先安装 JDK 17
    pause
    exit /b 1
)

echo [信息] 正在编译项目...
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo [错误] 编译失败
    pause
    exit /b 1
)

echo.
echo [信息] 正在启动服务...
echo.
call mvn spring-boot:run

pause
