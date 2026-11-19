#!/usr/bin/env bash
# Datos de ejemplo para la base de desarrollo, cargados a través de la propia API (no se escriben
# en la base directamente), para probar Swagger y el top-stock sin crear todo a mano.
# Requiere common.sh cargado y la API lista.

# El JSON va por la entrada estándar: así los nombres con tildes llegan en UTF-8 también en Windows.
post_json() {
  local path=$1 body=$2 response
  response=$(printf '%s' "$body" | curl -fsS -X POST "$API_URL$path" \
    -H 'Content-Type: application/json' --data-binary @-) || fail "No se pudieron cargar los datos de ejemplo ($path)"
  grep -o '"id":"[^"]*"' <<<"$response" | head -n 1 | cut -d'"' -f4
}

add_franchise() {
  post_json "/api/franchises" "{\"name\":\"$1\"}"
}

# add_branch <franquicia> <sucursal> <"producto:stock">...: crea la sucursal con sus productos.
add_branch() {
  local franchise=$1 branch_name=$2 branch product
  shift 2
  branch=$(post_json "/api/franchises/$franchise/branches" "{\"name\":\"$branch_name\"}")
  for product in "$@"; do
    post_json "/api/franchises/$franchise/branches/$branch/products" \
      "{\"name\":\"${product%:*}\",\"stock\":${product##*:}}" >/dev/null
  done
}

# Cada sucursal tiene un producto estrella (el primero, con mucho más stock que los demás): es el
# que debe aparecer en el top-stock de su franquicia.
seed_sample_data() {
  local cafe pizza
  info "Cargando datos de ejemplo (la base de desarrollo es nueva)"

  cafe=$(add_franchise "Café Andino")
  add_branch "$cafe" "Centro" "Café molido:80" "Chocolate de mesa:30" "Té verde:25"
  add_branch "$cafe" "Norte" "Galletas de avena:70" "Café molido:20" "Té verde:15"
  add_branch "$cafe" "Sur" "Chocolate de mesa:65" "Café molido:35" "Galletas de avena:10"

  pizza=$(add_franchise "Pizzería Napoli")
  add_branch "$pizza" "Chapinero" "Queso mozzarella:90" "Masa madre:40" "Salsa de tomate:30"
  add_branch "$pizza" "Usaquén" "Masa madre:75" "Queso mozzarella:30" "Albahaca fresca:12"
  add_branch "$pizza" "Teusaquillo" "Salsa de tomate:60" "Masa madre:25" "Queso mozzarella:20"

  ok "2 franquicias, 6 sucursales y 18 productos de ejemplo (un producto estrella por sucursal)"
  printf '      Top-stock de Café Andino:     %s\n' "$API_URL/api/franchises/$cafe/top-stock-products"
  printf '      Top-stock de Pizzería Napoli: %s\n' "$API_URL/api/franchises/$pizza/top-stock-products"
}
