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
#   JWT_SECRET, JWT_EXPIRATION_MS, OPENAI_API_KEY, OPENAI_MODEL,
#   OPENAI_BASE_URL, CORS_ALLOWED_ORIGINS
#
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
exec java -jar "$ROOT/application/target/smarttrans-jakarta-bootable.jar"
