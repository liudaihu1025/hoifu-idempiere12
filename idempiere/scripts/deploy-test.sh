#!/bin/bash
set -e

echo "=== 开始部署测试环境 ==="

# 环境变量
TEST_SERVER="$TEST_SERVER"
DEPLOY_USER="deploy"
APP_NAME="idempiere"
DEPLOY_DIR="/opt/$APP_NAME"
BACKUP_DIR="/opt/backups/$APP_NAME"
BUILD_FILE="target/idempiere-server-${CI_COMMIT_SHORT_SHA}.zip"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 检查必要的文件
if [ ! -f "$BUILD_FILE" ]; then
    echo "错误: 构建文件 $BUILD_FILE 不存在"
    echo "当前目录内容:"
    ls -la target/ || echo "target目录不存在"
    exit 1
fi

echo "部署服务器: $TEST_SERVER"
echo "部署目录: $DEPLOY_DIR"
echo "构建版本: $CI_COMMIT_SHORT_SHA"
echo "构建文件大小: $(du -h $BUILD_FILE | cut -f1)"

# 创建备份
echo "创建备份..."
ssh $DEPLOY_USER@$TEST_SERVER "
    sudo mkdir -p $BACKUP_DIR
    if [ -d \"$DEPLOY_DIR\" ]; then
        echo '备份现有部署...'
        sudo tar -czf $BACKUP_DIR/backup_$TIMESTAMP.tar.gz $DEPLOY_DIR
        echo '备份完成: $BACKUP_DIR/backup_$TIMESTAMP.tar.gz'
    else
        echo '部署目录不存在，无需备份'
    fi
"

# 停止服务
echo "停止iDempiere服务..."
ssh $DEPLOY_USER@$TEST_SERVER "
    if sudo systemctl is-active --quiet $APP_NAME; then
        echo '停止运行中的服务...'
        sudo systemctl stop $APP_NAME
        echo '服务已停止'
    else
        echo '服务未运行'
    fi

    # 确保进程完全停止
    sleep 10
    if pgrep -f idempiere > /dev/null; then
        echo '强制停止残留进程...'
        sudo pkill -f idempiere
        sleep 5
    fi
"

# 上传新版本
echo "上传新版本..."
scp $BUILD_FILE $DEPLOY_USER@$TEST_SERVER:/tmp/idempiere-new.zip

# 部署新版本
echo "部署新版本..."
ssh $DEPLOY_USER@$TEST_SERVER "
    set -e

    echo '清理部署目录...'
    sudo rm -rf $DEPLOY_DIR.bak
    if [ -d \"$DEPLOY_DIR\" ]; then
        sudo mv $DEPLOY_DIR $DEPLOY_DIR.bak
    fi

    echo '创建新目录并解压...'
    sudo mkdir -p $DEPLOY_DIR
    echo '解压文件...'
    sudo unzip -q /tmp/idempiere-new.zip -d $DEPLOY_DIR/
    sudo rm -f /tmp/idempiere-new.zip

    echo '设置权限...'
    sudo chmod +x $DEPLOY_DIR/*.sh
    sudo chown -R $APP_NAME:$APP_NAME $DEPLOY_DIR

    echo '恢复配置和数据...'
    if [ -d \"$DEPLOY_DIR.bak\" ]; then
        # 恢复配置文件
        if [ -f \"$DEPLOY_DIR.bak/idempiere.properties\" ]; then
            echo '恢复配置文件...'
            sudo cp $DEPLOY_DIR.bak/idempiere.properties $DEPLOY_DIR/
        fi

        # 恢复数据目录
        if [ -d \"$DEPLOY_DIR.bak/data\" ]; then
            echo '恢复数据目录...'
            sudo cp -r $DEPLOY_DIR.bak/data $DEPLOY_DIR/
        fi

        # 恢复日志配置
        if [ -f \"$DEPLOY_DIR.bak/log4j2.xml\" ]; then
            echo '恢复日志配置...'
            sudo cp $DEPLOY_DIR.bak/log4j2.xml $DEPLOY_DIR/
        fi
    else
        echo '没有旧版本目录，使用默认配置'
    fi

    echo '部署文件完成'
"

# 启动服务
echo "启动服务..."
ssh $DEPLOY_USER@$TEST_SERVER "
    sudo systemctl start $APP_NAME
    echo '服务启动命令已执行'
"

# 等待并检查服务状态
echo "等待服务启动..."
sleep 30

# 健康检查
echo "执行健康检查..."
MAX_RETRIES=10
RETRY_COUNT=0
HEALTH_CHECK_PASSED=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if ssh $DEPLOY_USER@$TEST_SERVER "curl -s -f http://localhost:8080/webui/ > /dev/null"; then
        # 额外检查：确保页面包含iDempiere关键词
        if ssh $DEPLOY_USER@$TEST_SERVER "curl -s http://localhost:8080/webui/ | grep -i 'idempiere' > /dev/null"; then
            HEALTH_CHECK_PASSED=true
            echo "✓ 服务健康检查通过"
            break
        fi
    fi

    RETRY_COUNT=$((RETRY_COUNT+1))
    echo "⏳ 服务尚未就绪，等待中... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 30
done

if [ "$HEALTH_CHECK_PASSED" = false ]; then
    echo "✗ 服务健康检查失败"

    # 显示日志以帮助调试
    echo "=== 显示服务状态和日志 ==="
    ssh $DEPLOY_USER@$TEST_SERVER "
        echo '服务状态:'
        sudo systemctl status $APP_NAME --no-pager || echo '无法获取服务状态'
        echo '最近日志:'
        sudo journalctl -u $APP_NAME --no-pager -n 50 || echo '无法获取日志'
        echo '进程信息:'
        ps aux | grep idempiere | grep -v grep || echo '没有找到相关进程'
    "
    exit 1
fi

# 显示部署信息
echo "=== 部署完成 ==="
echo "应用地址: http://$TEST_SERVER:8080/webui"
echo "服务状态:"
ssh $DEPLOY_USER@$TEST_SERVER "sudo systemctl status $APP_NAME --no-pager | head -10"

echo "=== 测试环境部署成功 ==="