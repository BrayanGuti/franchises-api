# ADR-002: Modelo de datos en DynamoDB

**Estado**: aceptada

## Contexto

Todos los accesos parten de una franquicia: agregarle sucursales, agregar productos a una sucursal
y consultar el producto con más stock de cada sucursal. DynamoDB no tiene joins ni unicidad, así que
el modelo se diseña para esos accesos.

## Opciones evaluadas

| Tema | Opciones | Elegida |
|---|---|---|
| Tablas | Una por entidad · un documento por franquicia con listas · **tabla única con un ítem por entidad** | Una consulta trae la franquicia completa y cada producto se modifica solo (sin reescribir listas ni llegar al límite de 400 KB) |
| Identificadores | Elegidos por el cliente · secuencias · **UUID generados** | Únicos sin coordinación; el nombre se puede cambiar libremente |
| Nombre de producto único | Sin unicidad · ID derivado del nombre · **ítem de reserva** | Unicidad atómica sin atar el ID al nombre |

## Decisión

Una tabla `franchises` (on-demand) con clave compuesta:

| Entidad | PK | SK |
|---|---|---|
| Franquicia | `F#{franchiseId}` | `META` |
| Sucursal | `F#{franchiseId}` | `B#{branchId}` |
| Producto | `F#{franchiseId}` | `B#{branchId}#P#{productId}` |
| Reserva de nombre | `F#{franchiseId}` | `B#{branchId}#NAME#{nombre normalizado}` |

- **UUID** validados en la entrada con `@ValidId`: un ID mal formado responde 400 sin tocar la base.
- **Reserva de nombre**: se crea en la misma transacción que el producto con `attribute_not_exists`;
  si dos peticiones intentan el mismo nombre a la vez, una gana y la otra recibe 409. La
  normalización ignora mayúsculas, tildes y espacios sobrantes ("Café" = "cafe" = "  CAFÉ ").
- **Producto con más stock por sucursal**: una sola consulta paginada por `PK` con `ConsistentRead`;
  la regla vive en el caso de uso. Con empate se devuelven todos los empatados, las sucursales sin
  productos se omiten y el orden es estable (sucursal y luego producto).

## Consecuencias

- El top-stock cuesta una consulta por franquicia, sin índices adicionales.
- Las consultas por prefijo de `SK` traen varios tipos de ítem, que se filtran por `entityType`.
- Eliminar o renombrar un producto debe mover su reserva ([ADR-003](ADR-003-consistencia.md)).
