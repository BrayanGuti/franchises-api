#!/usr/bin/env bash
# Entorno de desarrollo completo: dynamodb-dev + inspector (dynamodb-admin) + la API en perfil dev.
# Cuando la API está lista muestra y abre Swagger y el inspector, y sigue los logs de la API.

# shellcheck source=lib/common.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"
# shellcheck source=lib/seed.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/seed.sh"

usage() {
  cat <<'EOF'
Uso: scripts/dev.sh [opciones]

Levanta dynamodb-dev, el inspector de la base y la API (perfil dev). Cuando la API está lista
muestra y abre Swagger y el inspector, y deja los logs de la API en esta terminal.
La primera vez (base de desarrollo vacía) carga datos de ejemplo. Los datos se conservan.

Opciones:
  --maven        La API corre con Maven, sin Docker (requiere el JDK 21). Por defecto corre en Docker.
  --seed         Carga los datos de ejemplo aunque la base ya tenga datos.
  --no-seed      No carga los datos de ejemplo.
  --no-browser   No abre el navegador.
  -h, --help     Muestra esta ayuda.

Para detener todo: scripts/stop.sh
EOF
}

mode=docker
seed=auto
while [[ $# -gt 0 ]]; do
  case $1 in
    --maven) mode=maven ;;
    --seed) seed=always ;;
    --no-seed) seed=never ;;
    --no-browser) NO_BROWSER=1 ;;
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

require_docker
if [[ $mode == maven ]]; then
  require_java21
fi

# Se decide antes de levantar dynamodb-dev, porque docker compose crea el volumen si no existe.
if [[ $seed == auto ]]; then
  seed=never
  docker volume inspect "$DEV_VOLUME" >/dev/null 2>&1 || seed=always
fi

# Se ejecuta una vez, cuando la API responde: datos de ejemplo, direcciones y navegador.
on_api_ready() {
  if [[ $seed == always ]]; then
    seed_sample_data
  fi
  print_urls
  open_in_browser "$SWAGGER_URL"
  open_in_browser "$INSPECTOR_URL"
}

start_in_docker() {
  if responds "$API_URL" && ! app_container_running; then
    fail "El puerto 8080 está ocupado (¿la API con --maven en otra terminal?). Detenla y vuelve a intentar."
  fi
  # --build reconstruye solo lo que cambió (caché de capas): la API siempre corre el código actual.
  info "Construyendo y arrancando la API en Docker (la primera vez tarda unos minutos)"
  docker compose --profile app up -d --build app
  wait_until "API lista" 300 api_is_ready
  on_api_ready

  # Ctrl+C solo deja de mostrar los logs: el contenedor sigue corriendo.
  trap 'printf "\n"; info "La API sigue corriendo en Docker. Para detener todo: scripts/stop.sh"; exit 0' INT
  info "Logs de la API (Ctrl+C deja de mostrarlos; la API sigue corriendo)"
  docker compose --profile app logs --follow --tail 15 app
}

start_with_maven() {
  if app_container_running; then
    fail "La API ya corre en Docker: usa scripts/dev.sh sin --maven, o detenla antes con scripts/stop.sh."
  fi
  if api_is_ready; then
    ok "La API ya está corriendo"
    on_api_ready
    exit 0
  fi
  if responds "$API_URL"; then
    fail "El puerto 8080 está ocupado por otra aplicación. Libéralo y vuelve a intentar."
  fi

  # La API ocupa esta terminal: un proceso en segundo plano espera a que esté lista.
  (
    wait_until "API lista" 300 api_is_ready
    on_api_ready
  ) &
  waiter=$!
  trap 'kill "$waiter" 2>/dev/null || true' EXIT

  info "Arrancando la API con Maven (Ctrl+C la detiene; las bases siguen corriendo)"
  ./mvnw -q spring-boot:run
}

info "Levantando dynamodb-dev y el inspector"
docker compose --profile tools up -d dynamodb-dev dynamodb-admin
wait_for_service "$DYNAMODB_DEV_URL" "dynamodb-dev"
wait_for_service "$INSPECTOR_URL" "Inspector"

if [[ $mode == docker ]]; then
  start_in_docker
else
  start_with_maven
fi
