#!/usr/bin/env bash
# Corre todas las pruebas (unitarias + integración contra dynamodb-test) con el umbral de cobertura,
# muestra el resumen de cobertura y abre el reporte.

# shellcheck source=lib/common.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

usage() {
  cat <<'EOF'
Uso: scripts/test.sh [opciones] [argumentos de Maven]

Levanta dynamodb-test (en memoria), ejecuta ./mvnw verify y abre el reporte de cobertura.
Los argumentos que no son opciones se pasan a Maven.

Opciones:
  --no-browser   No abre el reporte de cobertura.
  -h, --help     Muestra esta ayuda.

Ejemplos:
  scripts/test.sh
  scripts/test.sh -Dit.test='*ConcurrencyIT' -Djacoco.haltOnFailure=false
EOF
}

maven_args=()
while [[ $# -gt 0 ]]; do
  case $1 in
    --no-browser) NO_BROWSER=1 ;;
    -h | --help)
      usage
      exit 0
      ;;
    *) maven_args+=("$1") ;;
  esac
  shift
done

REPORT_DIR="target/site/jacoco"

# Resumen de líneas y ramas cubiertas a partir del CSV de JaCoCo.
print_coverage() {
  awk -F, 'NR > 1 { lm += $8; lc += $9; bm += $6; bc += $7 }
    END {
      if (lc + lm > 0) printf "  Cobertura de líneas  %.1f %%\n", 100 * lc / (lc + lm)
      if (bc + bm > 0) printf "  Cobertura de ramas   %.1f %%\n", 100 * bc / (bc + bm)
    }' "$REPORT_DIR/jacoco.csv"
}

require_docker
require_java21

info "Levantando dynamodb-test"
docker compose up -d dynamodb-test
wait_for_service "$DYNAMODB_TEST_URL" "dynamodb-test"

# Se borra el reporte anterior para no abrir uno viejo si el build falla antes de generarlo.
rm -rf "$REPORT_DIR"

info "Ejecutando ./mvnw verify ${maven_args[*]:-}"
status=0
./mvnw -B verify ${maven_args[@]+"${maven_args[@]}"} || status=$?

if [[ -f $REPORT_DIR/jacoco.csv ]]; then
  printf '\n'
  print_coverage
  printf '  Reporte              %s/index.html\n\n' "$REPORT_DIR"
  open_in_browser "$REPORT_DIR/index.html"
fi

if [[ $status -ne 0 ]]; then
  fail "El build falló. Detalle en target/surefire-reports y target/failsafe-reports."
fi
ok "Pruebas y umbral de cobertura en verde"
