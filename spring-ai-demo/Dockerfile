# syntax=docker/dockerfile:1
# Build at repo root: docker compose build backend
# Maven mirror: Aliyun public (see docker/maven-settings-aliyun.xml); override by replacing that file.

FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

RUN mkdir -p /root/.m2
COPY spring-ai-demo/docker/maven-settings-aliyun.xml /root/.m2/settings.xml

COPY pom.xml .
COPY spring-ai-demo/pom.xml spring-ai-demo/pom.xml
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -pl spring-ai-demo -am dependency:go-offline -Dmaven.test.skip=true

COPY spring-ai-demo ./spring-ai-demo
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -T 1C -pl spring-ai-demo -am package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app \
    && apk add --no-cache su-exec
COPY spring-ai-demo/docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
COPY --from=builder /build/spring-ai-demo/target/spring-ai-demo-*.jar /app/app.jar
RUN chown app:app /app/app.jar
EXPOSE 8080
# Entrypoint runs as root briefly to chown bind mounts, then drops to app (see docker-entrypoint.sh)
ENTRYPOINT ["/docker-entrypoint.sh"]
