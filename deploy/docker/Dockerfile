# ============================================================================
# 校园保卫 SaaS API 镜像（仅配置定义，构建/运行命令见《部署与运维.md》说明）
# 构建上下文：仓库根目录
#   docker build -f deploy/docker/Dockerfile -t bwc-api:1.0.0 .
# ============================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    SERVER_PORT=8080 \
    BWC_PDF_FONT=/usr/share/fonts/chinese/stsong.ttf \
    JAVA_OPTS="-Xms512m -Xmx1536m -XX:+UseG1GC -Dfile.encoding=UTF-8"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/* \
    && ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && groupadd --system bwc \
    && useradd --system --gid bwc --home /opt/bwc --shell /usr/sbin/nologin bwc \
    && mkdir -p /opt/bwc/uploads /opt/bwc/logs /usr/share/fonts/chinese \
    && chown -R bwc:bwc /opt/bwc

WORKDIR /opt/bwc
COPY --from=build /build/target/bwc-api.jar ./bwc-api.jar
COPY sql ./sql

USER bwc
EXPOSE 8080
VOLUME ["/opt/bwc/uploads", "/usr/share/fonts/chinese"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/api/public/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/bwc/bwc-api.jar"]
