# Stage 1: Build (using local Gradle wrapper if available, otherwise skip)
# If you want to build inside Docker, use: FROM gradle:8.10-jdk24 AS build
# For now, assume JAR is built externally and copy it directly

# Stage 2: Run
FROM eclipse-temurin:24-jdk

WORKDIR /app

# Устанавливаем часовой пояс и curl для health check
ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Устанавливаем curl
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Копируем JAR файл (предполагается, что он лежит рядом с Dockerfile)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]
