#!/usr/bin/env bash
# Funciones compartidas por los scripts de desarrollo. No se ejecuta sola: cada script la carga con
#   source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"
# Funciona en Linux, macOS y Windows (Git Bash o WSL).

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# Las usan los scripts que cargan este archivo.
# shellcheck disable=SC2034
{
  API_URL="http://localhost:8080"
  SWAGGER_URL="$API_URL/swagger-ui.html"
  READINESS_URL="$API_URL/actuator/health/readiness"
  INSPECTOR_URL="http://127.0.0.1:8002"
  DYNAMODB_DEV_URL="http://127.0.0.1:8000"
  DYNAMODB_TEST_URL="http://127.0.0.1:8001"
  # Volumen de dynamodb-dev (proyecto "franchises-api" de docker-compose.yml): si no existe, la base es nueva.
  DEV_VOLUME="franchises-api_dynamodb-dev-data"
}

# Colores solo en una terminal y si NO_COLOR no está definido (https://no-color.org).
if [[ -t 1 && -z ${NO_COLOR:-} ]]; then
  BLUE=$'\033[1;34m' GREEN=$'\033[1;32m' YELLOW=$'\033[1;33m' RED=$'\033[1;31m' RESET=$'\033[0m'
else
  BLUE='' GREEN='' YELLOW='' RED='' RESET=''
fi

info() { printf '%s==>%s %s\n' "$BLUE" "$RESET" "$*"; }
ok() { printf '%s  ✔%s %s\n' "$GREEN" "$RESET" "$*"; }
warn() { printf '%s  !%s %s\n' "$YELLOW" "$RESET" "$*" >&2; }
fail() {
  printf '%s  ✖%s %s\n' "$RED" "$RESET" "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Falta '$1': $2"
}

require_docker() {
  require_command curl "instálalo con el gestor de paquetes de tu sistema."
  require_command docker "instala Docker Desktop (https://docs.docker.com/get-docker/)."
  docker info >/dev/null 2>&1 || fail "Docker no está corriendo: inicia Docker Desktop (o el servicio docker) y reintenta."
  docker compose version >/dev/null 2>&1 || fail "Falta Docker Compose v2 ('docker compose')."
}

# mvnw usa JAVA_HOME si está definido; si no, el java del PATH. Ambos deben ser Java 21 o superior.
require_java21() {
  local java="java" version
  if [[ -n ${JAVA_HOME:-} ]]; then
    java="$JAVA_HOME/bin/java"
  fi
  command -v "$java" >/dev/null 2>&1 || fail "No se encontró Java. Instala el JDK 21 (https://adoptium.net) y apunta JAVA_HOME a él, o usa Docker."
  version=$("$java" -version 2>&1 | awk -F'"' '/version/ { print $2; exit }' | cut -d. -f1)
  if [[ ! $version =~ ^[0-9]+$ || $version -lt 21 ]]; then
    fail "Se requiere Java 21 o superior; se encontró ${version:-una versión desconocida} en $(command -v "$java").
     Apunta JAVA_HOME al JDK 21 (export JAVA_HOME=\"/ruta/al/jdk-21\") o usa Docker."
  fi
}

# wait_until <descripción> <segundos> <comando...>: repite el comando cada segundo hasta que funcione.
wait_until() {
  local description=$1 timeout=$2
  shift 2
  local second
  for ((second = 0; second < timeout; second++)); do
    if "$@" >/dev/null 2>&1; then
      ok "$description"
      return 0
    fi
    sleep 1
  done
  fail "$description: sin respuesta en ${timeout} s"
}

# Cualquier respuesta HTTP sirve: solo se comprueba que el servicio acepte conexiones.
responds() {
  curl -s -o /dev/null --max-time 2 "$1"
}

api_is_ready() {
  curl -fs --max-time 2 "$READINESS_URL" | grep -q '"UP"'
}

# La API corre en su contenedor (perfil app de docker-compose.yml).
app_container_running() {
  [[ -n $(docker compose --profile app ps --status running --quiet app 2>/dev/null) ]]
}

wait_for_service() {
  wait_until "$2 responde ($1)" "${3:-60}" responds "$1"
}

# Abre una URL o un archivo en el navegador. NO_BROWSER=1 (o --no-browser) lo desactiva.
# Nunca detiene el script: si no se puede abrir, se muestra la dirección para abrirla a mano.
open_in_browser() {
  open_with_system "$1" || info "Abre en el navegador: $1"
}

open_with_system() {
  local target=$1
  if [[ ${NO_BROWSER:-0} == 1 ]]; then
    return 0
  fi
  if [[ -e $target ]]; then
    target=$(native_path "$target")
  fi
  if [[ $(uname -s) == Darwin ]]; then
    open "$target"
  elif command -v cmd.exe >/dev/null 2>&1; then
    # Windows (Git Bash o WSL). Desde WSL, cmd.exe avisa que no soporta rutas UNC: se silencia.
    # MSYS_NO_PATHCONV evita que Git Bash convierta "/c" en "C:/".
    (cd / && MSYS_NO_PATHCONV=1 cmd.exe /c start "" "$target") >/dev/null 2>&1
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$target" >/dev/null 2>&1 &
  else
    return 1
  fi
}

# Ruta de archivo que entiende el navegador del sistema (en Windows, C:\...).
native_path() {
  local absolute
  absolute="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -w "$absolute"
  elif command -v wslpath >/dev/null 2>&1; then
    wslpath -w "$absolute"
  else
    echo "$absolute"
  fi
}

print_urls() {
  printf '\n'
  ok "Swagger funcionando en $SWAGGER_URL  (prueba la API aquí)"
  ok "Inspector de la base en $INSPECTOR_URL  (tablas e ítems de dynamodb-dev)"
  ok "API en $API_URL  (estado: $API_URL/actuator/health)"
  printf '\n'
}
