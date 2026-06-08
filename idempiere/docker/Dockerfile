# Dockerfile
FROM maven:3.8.5-openjdk-17 AS builder

# 安装构建依赖
RUN apt-get update && apt-get install -y \
    postgresql-client \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# 复制源码
WORKDIR /build
COPY . .

# 构建iDempiere (跳过测试加快构建速度)
RUN mvn clean install -DskipTests -Dmaven.test.skip=true

# 运行时镜像
FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y \
    postgresql-client \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -m -s /bin/bash idempiere

USER idempiere
WORKDIR /opt/idempiere_docker

# 从构建阶段复制构建产物
COPY --from=builder --chown=idempiere:idempiere /build/org.adempiere.server-feature/target/*.zip /tmp/idempiere.zip

# 解压并配置
RUN unzip -q /tmp/idempiere.zip -d /opt/idempiere_docker/ \
    && rm /tmp/idempiere.zip \
    && chmod +x /opt/idempiere_docker/*.sh \
    && mkdir -p /opt/idempiere_docker/logs /opt/idempiere_docker/data

# 复制数据库等待脚本
COPY --chown=idempiere:idempiere scripts/wait-for-db.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/wait-for-db.sh

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s \
    CMD curl -f http://localhost:8080/webui/ || exit 1

EXPOSE 8082 8444

# 启动命令 - 等待数据库就绪后启动iDempiere
CMD ["/bin/bash", "-c", "/usr/local/bin/wait-for-db.sh && ./RUN_Server2.sh"]