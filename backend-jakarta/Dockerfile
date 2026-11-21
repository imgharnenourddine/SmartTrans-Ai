# Build from the repository root:
#   docker build -f backend-jakarta/Dockerfile -t smarttrans-jakarta:local .
#
# Start PostgreSQL with its current Compose file, then run this image on that network:
#   docker compose -f docker/postgres/compose.yml up -d
#   docker run --rm --name smarttrans-jakarta \
#     --network postgres_default \
#     -p 127.0.0.1:8080:8080 \
#     -e POSTGRESQL_URL=jdbc:postgresql://postgres:5432/smarttrans \
#     -e POSTGRESQL_USER="${POSTGRES_USER:-smarttrans}" \
#     -e POSTGRESQL_PASSWORD \
#     -e JWT_SECRET \
#     -e MISTRAL_API_KEY \
#     smarttrans-jakarta:local
#
# POSTGRESQL_PASSWORD must be the same value as POSTGRES_PASSWORD.
# Pass it from the host environment, or with --env-file. Do not put it in this image.
# JWT_EXPIRATION_MS, MISTRAL_MODEL, MISTRAL_BASE_URL, MISTRAL_REQUEST_TIMEOUT_MS
# and CORS_ALLOWED_ORIGINS are optional.
# Omit JWT_SECRET and MISTRAL_API_KEY, or set them to a non-empty value.
# An empty string is rejected when the deployment starts.

FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /src
COPY backend-jakarta/pom.xml backend-jakarta/pom.xml
COPY backend-jakarta/launcher/pom.xml backend-jakarta/launcher/pom.xml
COPY backend-jakarta/application/pom.xml backend-jakarta/application/pom.xml
COPY backend-jakarta/launcher/src backend-jakarta/launcher/src
COPY backend-jakarta/application/src backend-jakarta/application/src
RUN mvn -f backend-jakarta/pom.xml -B -DskipTests package

FROM eclipse-temurin:25-jre
RUN groupadd --system smarttrans \
    && useradd --system --gid smarttrans --home-dir /opt/smarttrans smarttrans
WORKDIR /opt/smarttrans
COPY --from=build --chown=smarttrans:smarttrans \
    /src/backend-jakarta/launcher/target/smarttrans-launcher.jar \
    backend-jakarta/launcher/target/smarttrans-launcher.jar
COPY --from=build --chown=smarttrans:smarttrans \
    /src/backend-jakarta/launcher/target/lib \
    backend-jakarta/launcher/target/lib
COPY --from=build --chown=smarttrans:smarttrans \
    /src/backend-jakarta/application/target/smarttrans-jakarta-bootable.jar \
    backend-jakarta/application/target/smarttrans-jakarta-bootable.jar
COPY --chown=smarttrans:smarttrans backend-jakarta/start.sh backend-jakarta/start.sh
COPY --chown=smarttrans:smarttrans \
    backend/src/main/resources/db/migration \
    backend/src/main/resources/db/migration
USER smarttrans
ENV SKIP_BUILD=1
# WildFly listens on 127.0.0.1 by default. The published container port
# reaches the container network interface, so this image must bind every address.
# Replacing WILDFLY_JAVA_OPTS at runtime drops this setting.
ENV WILDFLY_JAVA_OPTS="-Djboss.bind.address=0.0.0.0"
EXPOSE 8080
ENTRYPOINT ["/opt/smarttrans/backend-jakarta/start.sh"]
