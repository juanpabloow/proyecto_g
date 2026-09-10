#!/usr/bin/env bash
#
# Compila y ejecuta el módulo Spring Boot de DAC.
# Requiere Maven instalado (mvn) y Java 21+.
# La base de datos debe estar corriendo: docker compose up -d
#
#   java/ejecutar.sh          # inicia el servidor en localhost:8080
#   java/ejecutar.sh build    # solo compila y ejecuta las pruebas
#
set -euo pipefail
cd "$(dirname "$0")"   # raíz del módulo java/

accion="${1:-run}"

case "$accion" in
  run)
    echo "=== Iniciando DAC en http://localhost:8080 ==="
    mvn spring-boot:run
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
    echo "Uso: java/ejecutar.sh [run|build|package]" >&2
    exit 2
    ;;
esac
