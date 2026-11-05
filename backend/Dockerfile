
# Stage 1: Build
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

COPY src/ src/

RUN ./mvnw -B -DskipTests package

# Stage 2: Runtime
FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd --system smarttrans \
    && useradd --system \
       --gid smarttrans \
       --home-dir /app \
       smarttrans

COPY --from=builder --chown=smarttrans:smarttrans \
    /app/target/smarttrans-ai-0.0.1-SNAPSHOT.jar \
    /app/app.jar

USER smarttrans

ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

