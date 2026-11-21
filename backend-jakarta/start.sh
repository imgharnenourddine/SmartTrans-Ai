#!/usr/bin/env bash
# Starts the Jakarta EE runtime.
# Flyway runs in its own process. WildFly is started only after Flyway exits 0.
#
# Required:
#   POSTGRESQL_URL        jdbc:postgresql://localhost:5432/smarttrans
#   POSTGRESQL_USER
#   POSTGRESQL_PASSWORD
#
# Optional:
#   FLYWAY_MIGRATIONS_PATH   defaults to ../backend/src/main/resources/db/migration
#   SKIP_BUILD=1             reuse the already built jars
#   WILDFLY_JAVA_OPTS        JVM arguments for WildFly, for example a system property
#   JWT_SECRET, JWT_EXPIRATION_MS, OPENAI_API_KEY, OPENAI_MODEL,
#   OPENAI_BASE_URL, CORS_ALLOWED_ORIGINS
#
# MicroProfile Config priority, highest first: Java system properties set through
# WILDFLY_JAVA_OPTS, then environment variables, then META-INF/microprofile-config.properties.
# POSTGRESQL_URL, POSTGRESQL_USER and POSTGRESQL_PASSWORD are read by Flyway and by
# the WildFly datasource expression resolver. They are not MicroProfile Config properties.
# POSTGRESQL_JNDI is fixed by this script to java:/jdbc/SmartTransDS.
# The PostgreSQL Compose file is not invoked here. Start that database first.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$ROOT/.." && pwd)"

: "${POSTGRESQL_URL:?POSTGRESQL_URL is required}"
: "${POSTGRESQL_USER:?POSTGRESQL_USER is required}"
: "${POSTGRESQL_PASSWORD:?POSTGRESQL_PASSWORD is required}"

export POSTGRESQL_JNDI="java:/jdbc/SmartTransDS"
export POSTGRESQL_JTA="true"
export FLYWAY_MIGRATIONS_PATH="${FLYWAY_MIGRATIONS_PATH:-$REPO/backend/src/main/resources/db/migration}"

if [[ "${SKIP_BUILD:-}" != "1" ]]; then
  mvn -f "$ROOT/pom.xml" -DskipTests package
fi

java -jar "$ROOT/launcher/target/smarttrans-launcher.jar"

wildfly_args=()
if [[ -n "${WILDFLY_JAVA_OPTS:-}" ]]; then
  read -r -a wildfly_args <<< "${WILDFLY_JAVA_OPTS}"
fi
exec java "${wildfly_args[@]}" -jar "$ROOT/application/target/smarttrans-jakarta-bootable.jar"
