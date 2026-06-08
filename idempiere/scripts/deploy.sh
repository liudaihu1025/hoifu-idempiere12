#!/bin/bash
set -e

echo "Starting iDempiere deployment..."

# 变量定义
DEPLOY_DIR="/opt/idempiere"
BACKUP_DIR="/opt/idempiere-backup"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 创建备份
echo "Creating backup..."
sudo mkdir -p $BACKUP_DIR
if [ -d "$DEPLOY_DIR" ]; then
    sudo tar -czf $BACKUP_DIR/idempiere-backup-$TIMESTAMP.tar.gz $DEPLOY_DIR
fi

# 停止服务
echo "Stopping iDempiere service..."
sudo systemctl stop idempiere || true

# 部署新版本
echo "Deploying new version..."
sudo mkdir -p $DEPLOY_DIR
sudo cp -r /tmp/idempiere/* $DEPLOY_DIR/
sudo chmod +x $DEPLOY_DIR/*.sh

# 运行安装脚本
echo "Running setup..."
cd $DEPLOY_DIR
sudo sh RUN_setup.sh

# 设置权限
sudo chown -R idempiere:idempiere $DEPLOY_DIR

# 启动服务
echo "Starting iDempiere service..."
sudo systemctl start idempiere

echo "Deployment completed successfully!"