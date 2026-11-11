#!/usr/bin/env bash
#
# Atajo sobre el Maven Wrapper de la raíz (./mvnw): no necesita Maven
# instalado, solo Java 21+. Equivale a los comandos de la guía:
#   ./mvnw spring-boot:run   y   ./mvnw verify
# La base de datos debe estar corriendo: docker compose up -d
#
#   java/ejecutar.sh          # inicia el servidor en localhost:8080 con la
#                             # credencial de .env (DAC_AUTH_USER/DAC_AUTH_PASSWORD)
#   java/ejecutar.sh dev      # igual, con el perfil de desarrollo: credencial
#                             # DEMO analista / dac-demo-2026 (solo local)
#   java/ejecutar.sh build    # compila y corre la suite completa (./mvnw verify)
#
# Otro puerto: SERVER_PORT=8090 java/ejecutar.sh
#
set -euo pipefail
cd "$(dirname "$0")/.."   # raíz del proyecto, donde está ./mvnw

accion="${1:-run}"

case "$accion" in
  run)
    echo "=== Iniciando DAC en http://localhost:${SERVER_PORT:-8080} (credencial de .env) ==="
    ./mvnw spring-boot:run
    ;;
  dev)
    echo "=== Iniciando DAC en http://localhost:${SERVER_PORT:-8080} (perfil dev: analista / dac-demo-2026) ==="
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
    ;;
  build)
    echo "=== Compilando y corriendo la suite completa ==="
    ./mvnw verify
    ;;
  package)
    echo "=== Generando JAR ==="
    ./mvnw package -DskipTests
    echo "JAR generado en java/target/dac-0.0.1-SNAPSHOT.jar"
    ;;
  *)
    echo "Uso: java/ejecutar.sh [run|dev|build|package]" >&2
    exit 2
    ;;
esac
