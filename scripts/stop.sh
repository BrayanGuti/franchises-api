#!/usr/bin/env bash
# Detiene los contenedores del entorno local. Con --clean borra también los datos de desarrollo.

# shellcheck source=lib/common.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

usage() {
  cat <<'EOF'
Uso: scripts/stop.sh [opciones]

Detiene las bases, el inspector y la API en Docker. Los datos de dynamodb-dev se conservan.

Opciones:
  --clean        Borra también los datos de dynamodb-dev (pide confirmación).
  -y, --yes      No pide confirmación (para usar --clean sin terminal interactiva).
  -h, --help     Muestra esta ayuda.

La API que corre con Maven (scripts/dev.sh --maven) se detiene con Ctrl+C en su terminal.
EOF
}

clean=false
assume_yes=false
while [[ $# -gt 0 ]]; do
  case $1 in
    --clean) clean=true ;;
    -y | --yes) assume_yes=true ;;
    -h | --help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      fail "Opción desconocida: $1"
      ;;
  esac
  shift
done

confirm_clean() {
  if [[ $assume_yes == true ]]; then
    return 0
  fi
  [[ -t 0 ]] || fail "--clean borra los datos de desarrollo: confirma con --yes si no hay terminal interactiva."
  local answer
  read -r -p "Se borrarán todas las franquicias de dynamodb-dev. ¿Continuar? [s/N] " answer
  [[ $answer =~ ^[sS]$ ]] || fail "Cancelado; no se borró nada."
}

require_docker

# Los perfiles incluyen el inspector (tools) y la API en Docker (app).
compose=(docker compose --profile tools --profile app)

if [[ $clean == true ]]; then
  confirm_clean
  info "Deteniendo todo y borrando los datos de desarrollo"
  "${compose[@]}" down --volumes --remove-orphans
  ok "Contenedores detenidos y datos de dynamodb-dev borrados"
else
  info "Deteniendo todo (los datos de desarrollo se conservan)"
  "${compose[@]}" down --remove-orphans
  ok "Contenedores detenidos"
fi

if api_is_ready; then
  warn "La API sigue respondiendo en $API_URL: si corre con Maven, detenla con Ctrl+C en su terminal."
fi
