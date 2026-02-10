#!/bin/bash

# =====================================================
# English Word App 远程部署脚本
# 通过SSH连接到服务器进行部署
# =====================================================

set -e

# 服务器配置
SERVER_HOST="47.83.126.42"
SERVER_USER="root"
PROJECT_DIR="/root/english-word-app"
COMPOSE_FILE="$PROJECT_DIR/docker compose.yml"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# SSH执行命令
ssh_exec() {
    ssh ${SERVER_USER}@${SERVER_HOST} "$1"
}

# 显示使用说明
show_usage() {
    echo "Usage: ./remote-deploy.sh [command]"
    echo ""
    echo "Commands:"
    echo "  deploy  - 部署服务到远程服务器"
    echo "  start   - 启动远程服务"
    echo "  stop    - 停止远程服务"
    echo "  restart - 重启远程服务"
    echo "  status  - 查看远程服务状态"
    echo "  logs    - 查看远程服务日志"
    echo ""
}

# 部署服务
deploy() {
    log_info "开始部署到远程服务器 ${SERVER_HOST}..."

    # 1. 确保本地已打包
    if [ ! -f "target/english-word-backend-1.0.0.jar" ]; then
        log_info "正在打包项目..."
        mvn clean package -DskipTests
    fi

    # 2. 在远程服务器上创建项目目录
    log_info "创建远程项目目录..."
    ssh_exec "mkdir -p ${PROJECT_DIR}/{target,uploads}"

    # 3. 上传jar包
    log_info "上传jar包到服务器..."
    scp target/english-word-backend-1.0.0.jar ${SERVER_USER}@${SERVER_HOST}:${PROJECT_DIR}/target/

    # 4. 上传docker compose.yml
    log_info "上传docker compose.yml..."
    scp docker compose.yml ${SERVER_USER}@${SERVER_HOST}:${PROJECT_DIR}/

    # 5. 设置环境变量（如需要）
    log_info "设置环境变量..."
    if [ -n "$ZHIPUAI_API_KEY" ]; then
        ssh_exec "cd ${PROJECT_DIR} && export ZHIPUAI_API_KEY=${ZHIPUAI_API_KEY} && docker compose down && docker compose up -d"
    else
        log_warn "ZHIPUAI_API_KEY 未设置，使用服务器默认值"
        ssh_exec "cd ${PROJECT_DIR} && docker compose down && docker compose up -d"
    fi

    # 6. 等待服务启动
    log_info "等待服务启动..."
    sleep 15

    # 7. 检查服务状态
    status

    log_info "✅ 部署完成！"
    log_info "API地址: http://${SERVER_HOST}:8885/api"
}

# 启动服务
start() {
    log_info "启动远程服务..."
    ssh_exec "cd ${PROJECT_DIR} && docker compose up -d"
    log_info "✅ 服务已启动"
    sleep 5
    status
}

# 停止服务
stop() {
    log_info "停止远程服务..."
    ssh_exec "cd ${PROJECT_DIR} && docker compose down"
    log_info "✅ 服务已停止"
}

# 重启服务
restart() {
    log_info "重启远程服务..."
    ssh_exec "cd ${PROJECT_DIR} && docker compose restart"
    log_info "✅ 服务已重启"
    sleep 10
    status
}

# 查看状态
status() {
    log_info "远程服务状态:"
    ssh_exec "cd ${PROJECT_DIR} && docker compose ps"
}

# 查看日志
logs() {
    log_info "远程服务日志 (Ctrl+C退出):"
    if [ "$1" == "app" ]; then
        ssh_exec "cd ${PROJECT_DIR} && docker compose logs --tail=100 -f app"
    elif [ "$1" == "mysql" ]; then
        ssh_exec "cd ${PROJECT_DIR} && docker compose logs --tail=100 -f mysql"
    else
        ssh_exec "cd ${PROJECT_DIR} && docker compose logs --tail=100"
    fi
}

# 主函数
main() {
    case "$1" in
        deploy)
            deploy
            ;;
        start)
            start
            ;;
        stop)
            stop
            ;;
        restart)
            restart
            ;;
        status)
            status
            ;;
        logs)
            logs "$2"
            ;;
        *)
            show_usage
            ;;
    esac
}

main "$@"
