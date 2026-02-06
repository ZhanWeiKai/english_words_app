#!/bin/bash

echo "========================================"
echo "   English Word App - Backend 启动脚本"
echo "========================================"
echo ""

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null; then
    echo "[错误] 未检测到 Maven，请先安装 Maven"
    exit 1
fi

# 检查 Java 是否安装
if ! command -v java &> /dev/null; then
    echo "[错误] 未检测到 Java，请先安装 JDK 17"
    exit 1
fi

echo "[信息] 正在编译项目..."
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "[错误] 编译失败"
    exit 1
fi

echo ""
echo "[信息] 正在启动服务..."
echo ""
mvn spring-boot:run
