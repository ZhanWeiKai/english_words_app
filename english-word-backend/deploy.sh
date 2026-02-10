#!/bin/bash

# =====================================================
# English Word App 后端部署脚本
# =====================================================

set -e

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

# 显示使用说明
show_usage() {
    echo "Usage: ./deploy.sh [command]"
    echo ""
    echo "Commands:"
    echo "  deploy  - 部署服务"
    echo "  start   - 启动服务"
    echo "  stop    - 停止服务"
    echo "  restart - 重启服务"
    echo "  status  - 查看状态"
    echo "  logs    - 查看日志"
    echo "  clean   - 清理容器和卷"
    echo ""
}

# 检查环境变量
check_env() {
    if [ -z "$ZHIPUAI_API_KEY" ]; then
        log_warn "ZHIPUAI_API_KEY 环境变量未设置"
        log_info "请先设置: export ZHIPUAI_API_KEY=your_api_key"
        exit 1
    fi
}

# 部署服务
deploy() {
    log_info "开始部署 English Word App..."

    check_env

    # 1. 打包项目
    log_info "正在打包项目..."
    mvn clean package -DskipTests

    if [ ! -f "target/english-word-backend-1.0.0.jar" ]; then
        log_error "打包失败，找不到jar文件"
        exit 1
    fi

    # 2. 停止旧容器
    log_info "停止旧容器..."
    docker-compose down 2>/dev/null || true

    # 3. 创建必要的目录
    log_info "创建必要的目录..."
    mkdir -p uploads

    # 4. 启动服务
    log_info "启动服务..."
    docker-compose up -d

    # 5. 等待服务启动
    log_info "等待服务启动..."
    sleep 30

    # 6. 检查服务状态
    docker-compose ps

    # 7. 显示日志
    log_info "服务已部署，查看日志..."
    docker-compose logs --tail=50 app

    log_info "✅ 部署完成！"
    log_info "API文档: http://47.242.74.112:8885/api/swagger-ui.html"
}

# 启动服务
start() {
    log_info "启动服务..."
    check_env
    mkdir -p uploads
    docker-compose up -d
    log_info "✅ 服务已启动"
    docker-compose ps
}

# 停止服务
stop() {
    log_info "停止服务..."
    docker-compose down
    log_info "✅ 服务已停止"
}

# 重启服务
restart() {
    log_info "重启服务..."
    check_env
    docker-compose restart
    sleep 10
    log_info "✅ 服务已重启"
    docker-compose ps
}

# 查看状态
status() {
    log_info "服务状态:"
    docker-compose ps
    echo ""

    log_info "容器健康状态:"
    docker inspect --format='{{.State.Health.Status}}' english-word-mysql 2>/dev/null || echo "  MySQL: 容器不存在"
    docker inspect --format='{{.State.Health.Status}}' english-word-app 2>/dev/null || echo "  App: 容器不存在"
}

# 查看日志
logs() {
    if [ "$1" == "app" ]; then
        log_info "应用日志:"
        docker-compose logs --tail=100 -f app
    elif [ "$1" == "mysql" ]; then
        log_info "MySQL日志:"
        docker-compose logs --tail=100 -f mysql
    else
        log_info "所有服务日志:"
        docker-compose logs --tail=100
    fi
}

# 清理容器和卷
clean() {
    log_warn "即将删除容器和数据卷！"
    read -p "确认删除？(yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_info "已取消"
        exit 0
    fi

    log_info "停止并删除容器..."
    docker-compose down -v

    log_info "删除数据卷..."
    docker volume rm english-word-backend_mysql-data 2>/dev/null || true

    log_info "✅ 清理完成"
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
        clean)
            clean
            ;;
        *)
            show_usage
            ;;
    esac
}

main "$@"
