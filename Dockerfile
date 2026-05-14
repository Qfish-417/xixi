# 使用 Amazon Corretto 官方镜像
FROM amazoncorretto:17

WORKDIR /app

COPY target/manju-agent-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]