# =========================================================
# Multi-stage Dockerfile for the Feedback Tool backend
# =========================================================

# ---------- Stage 1: build ----------
FROM maven:3.8.7-eclipse-temurin-8 AS builder
WORKDIR /workspace

# 先拷贝 pom，预下载依赖以利用缓存
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DskipTests

COPY src ./src
RUN mvn -B -q -DskipTests clean package

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
