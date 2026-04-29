FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre

WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=25 -XX:+UseContainerSupport"

COPY --from=builder /workspace/build/libs/*.jar /app/notio-backend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/notio-backend.jar"]
