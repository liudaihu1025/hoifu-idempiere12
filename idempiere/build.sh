#!/bin/bash
set -e

# 构建脚本 - 针对Shell执行器优化

# Maven配置
MAVEN_CMD="mvn"
MAVEN_OPTS="-Dmaven.repo.local=/home/gitlab-runner/.m2/repository -Xmx2048m"
COMPILE_CMD="$MAVEN_CMD clean install -DskipTests -Dmaven.test.skip=true $MAVEN_OPTS"
TEST_CMD="$MAVEN_CMD test -DskipTests=false $MAVEN_OPTS"
PACKAGE_CMD="$MAVEN_CMD package -DskipTests -Dmaven.test.skip=true -P assembly $MAVEN_OPTS"

# 环境检查
check_environment() {
    echo "=== 检查构建环境 ==="

    # 检查Java
    if ! command -v java &> /dev/null; then
        echo "错误: Java未安装"
        exit 1
    fi
    echo "Java版本: $(java -version 2>&1 | head -n1)"

    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        echo "错误: Maven未安装"
        exit 1
    fi
    echo "Maven版本: $(mvn --version | head -n1)"

    # 检查磁盘空间
    echo "磁盘空间:"
    df -h /home/gitlab-runner

    # 检查内存
    echo "内存信息:"
    free -h

    echo "=== 环境检查完成 ==="
}

# 编译项目
compile() {
    echo "=== 开始编译 iDempiere ==="
    check_environment

    echo "当前目录: $(pwd)"
    echo "目录内容:"
    ls -la

    echo "执行编译命令: $COMPILE_CMD"

    # 设置Maven内存选项
    export MAVEN_OPTS="$MAVEN_OPTS"

    # 执行编译
    if $COMPILE_CMD; then
        echo "✓ 编译成功"
    else
        echo "✗ 编译失败"
        exit 1
    fi

    echo "=== 编译完成 ==="
}

# 运行测试
test() {
    echo "=== 开始运行测试 ==="

    echo "执行测试命令: $TEST_CMD"

    if $TEST_CMD; then
        echo "✓ 测试通过"
    else
        echo "✗ 测试失败"
        # 测试失败不终止构建，继续打包
        echo "测试失败，但继续打包流程..."
    fi

    echo "=== 测试完成 ==="
}

# 打包项目
package() {
    echo "=== 开始打包 iDempiere ==="

    # 确保先编译
    compile

    echo "执行打包命令: $PACKAGE_CMD"

    if $PACKAGE_CMD; then
        echo "✓ 打包成功"
    else
        echo "✗ 打包失败"
        exit 1
    fi

    # 查找生成的ZIP包
    echo "查找生成的包..."
    ZIP_FILE=$(find . -name "idempiereServer*.zip" -type f | head -1)
    if [ -z "$ZIP_FILE" ]; then
        echo "错误: 未找到生成的ZIP包"
        find . -name "*.zip" -type f
        exit 1
    fi

    echo "找到包: $ZIP_FILE"

    # 复制到target目录
    mkdir -p target
    cp "$ZIP_FILE" "target/idempiere-server-${CI_COMMIT_SHORT_SHA}.zip"

    # 显示包信息
    echo "包信息:"
    ls -lh "target/idempiere-server-${CI_COMMIT_SHORT_SHA}.zip"

    # 创建构建信息文件
    cat > target/build-info.txt << EOF
构建信息:
==========
项目: iDempiere 12
版本: ${CI_COMMIT_SHORT_SHA}
分支: ${CI_COMMIT_REF_NAME}
提交: ${CI_COMMIT_SHA}
构建时间: $(date)
构建用户: $(whoami)
构建服务器: $(hostname)

Java版本:
$(java -version 2>&1)

Maven版本:
$(mvn --version 2>&1 | head -n1)

系统信息:
$(uname -a)

磁盘空间:
$(df -h /home/gitlab-runner)

内存信息:
$(free -h)
EOF

    echo "生成的包: target/idempiere-server-${CI_COMMIT_SHORT_SHA}.zip"
    echo "=== 打包完成 ==="
}

# 清理构建缓存
clean_cache() {
    echo "=== 清理构建缓存 ==="

    # 清理Maven下载的临时文件，但保留repository
    find /home/gitlab-runner/.m2 -name "_remote.repositories" -type f -delete
    find /home/gitlab-runner/.m2 -name "*.lastUpdated" -type f -delete

    echo "缓存清理完成"
}

# 显示帮助
usage() {
    echo "用法: $0 {compile|test|package|clean-cache|help}"
    echo ""
    echo "命令说明:"
    echo "  compile     编译项目"
    echo "  test        运行测试"
    echo "  package     编译并打包项目"
    echo "  clean-cache 清理构建缓存"
    echo "  help        显示此帮助信息"
    exit 1
}

# 主逻辑
case "$1" in
    compile)
        compile
        ;;
    test)
        test
        ;;
    package)
        package
        ;;
    clean-cache)
        clean_cache
        ;;
    help)
        usage
        ;;
    *)
        echo "错误: 未知命令 '$1'"
        usage
        ;;
esac