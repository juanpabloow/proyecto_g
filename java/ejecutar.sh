#!/usr/bin/env bash
#
# Compila y ejecuta el módulo Spring Boot de DAC.
# Requiere Maven instalado (mvn) y Java 21+.
# La base de datos debe estar corriendo: docker compose up -d
#
#   java/ejecutar.sh          # inicia el servidor en localhost:8080 con la
#                             # credencial de .env (DAC_AUTH_USER/DAC_AUTH_PASSWORD)
#   java/ejecutar.sh dev      # igual, con el perfil de desarrollo: credencial
#                             # DEMO analista / dac-demo-2026 (solo local)
#   java/ejecutar.sh build    # solo compila y ejecuta las pruebas
#
# Otro puerto: SERVER_PORT=8090 java/ejecutar.sh
#
set -euo pipefail
cd "$(dirname "$0")"   # raíz del módulo java/

accion="${1:-run}"

case "$accion" in
  run)
    echo "=== Iniciando DAC en http://localhost:${SERVER_PORT:-8080} (credencial de .env) ==="
    mvn spring-boot:run
    ;;
  dev)
    echo "=== Iniciando DAC en http://localhost:${SERVER_PORT:-8080} (perfil dev: analista / dac-demo-2026) ==="
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ;;
  build)
    echo "=== Compilando y ejecutando pruebas ==="
    mvn test
    ;;
  package)
    echo "=== Generando JAR ==="
    mvn package -DskipTests
    echo "JAR generado en target/dac-0.0.1-SNAPSHOT.jar"
    ;;
  *)
    echo "Uso: java/ejecutar.sh [run|dev|build|package]" >&2
    exit 2
    ;;
esac
