#!/usr/bin/env bash
#
# Compila y ejecuta el módulo Java de DAC. Se corre desde cualquier lugar:
#
#   java/ejecutar.sh            # las pruebas y luego el flujo completo
#   java/ejecutar.sh pruebas    # solo las pruebas
#   java/ejecutar.sh main       # solo el flujo completo (acepta otro CSV)
#
set -euo pipefail

cd "$(dirname "$0")/.."   # raíz del repositorio: main.py espera data/ relativo
SALIDA=java/build
JAVA_OPTS=(-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8)

rm -rf "$SALIDA"
mkdir -p "$SALIDA"
find java/src java/test -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -d "$SALIDA"

accion="${1:-todo}"
shift || true

case "$accion" in
  pruebas) java "${JAVA_OPTS[@]}" -cp "$SALIDA" dac.pruebas.EjecutorDePruebas ;;
  main)    java "${JAVA_OPTS[@]}" -cp "$SALIDA" dac.Main "$@" ;;
  todo)
    java "${JAVA_OPTS[@]}" -cp "$SALIDA" dac.pruebas.EjecutorDePruebas
    echo
    echo "=== Flujo completo (data/contratos_ejemplo.csv) ==="
    java "${JAVA_OPTS[@]}" -cp "$SALIDA" dac.Main
    ;;
  *) echo "Uso: java/ejecutar.sh [pruebas|main|todo]" >&2; exit 2 ;;
esac
