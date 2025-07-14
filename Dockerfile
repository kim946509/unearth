FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/unearth-0.0.1-SNAPSHOT.jar app.jar
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "app.jar"]