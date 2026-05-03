# =========================================================
# Multi-stage Dockerfile for the Feedback Tool backend
# =========================================================

# ---------- Stage 1: build ----------
# syntax=docker/dockerfile:1.4
FROM maven:3.8.7-eclipse-temurin-8 AS builder
WORKDIR /workspace

# 把国内镜像 settings.xml 放到 maven 默认配置位置，强制走阿里云
COPY maven-settings.xml /root/.m2/settings.xml

# 先拷贝 pom，预下载依赖以利用层缓存 + BuildKit 持久化 ~/.m2 缓存
# 去掉 -q：保留进度输出，避免长时间下载看似"卡死"
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B dependency:go-offline -DskipTests

COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -DskipTests clean package

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:8-jre-jammy
LABEL org.opencontainers.image.source="https://github.com/your-org/feedback-tool"
LABEL maintainer="feedback-tool"

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx768m -Dfile.encoding=UTF-8" \
    SERVER_PORT=9091 \
    DB_HOST=mysql \
    DB_PORT=3306 \
    DB_NAME=feedback_1 \
    DB_USER=root \
    DB_PASSWORD=feedback123 \
    REDIS_HOST=redis \
    REDIS_PORT=6379 \
    FILES_UPLOAD_PATH=/data/files/

WORKDIR /app

# 安装 curl 用于健康检查
RUN apt-get update && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=builder /workspace/target/*.jar /app/app.jar

RUN mkdir -p /data/files && useradd -m runner && chown -R runner:runner /app /data
USER runner

EXPOSE 9091
# 只检查核心组件（ping + db + redis + diskSpace），避免 mail 等次要指标影响整体可用性
HEALTHCHECK --interval=15s --timeout=5s --retries=20 --start-period=60s \
  CMD curl -fsS http://localhost:${SERVER_PORT}/actuator/health/live || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
