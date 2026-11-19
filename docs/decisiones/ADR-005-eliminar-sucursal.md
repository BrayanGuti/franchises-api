# ADR-005: Eliminar una sucursal con lápida y barrido

**Estado**: aceptada · funcionalidad adicional al enunciado

## Contexto

Eliminar una sucursal implica borrar todos sus productos y reservas, que pueden ser más de los que
caben en una transacción de DynamoDB. Mientras tanto pueden llegar peticiones que agregan, renombran
o modifican productos de esa sucursal. La idea de partida fue: completar las peticiones en curso y,
una vez completadas, eliminar la sucursal y sus productos, sin dejar inconsistencias.

## Opciones evaluadas

| Opción | A favor | En contra |
|---|---|---|
| Esperar en memoria a que terminen las peticiones en curso | Intuitivo | No funciona con varias instancias de la API: cada una solo ve sus propias peticiones |
| Borrado lógico (`deleted = true`, filtrar en cada lectura, TTL) | Más simple de implementar | Cada lectura presente y futura debe acordarse de filtrar; los datos crecen; los nombres quedan reservados para siempre |
| **Lápida en la tabla + barrido** | El estado vive en DynamoDB, así que todas las instancias lo ven; borra los datos de verdad | Más pasos |

## Decisión

1. **Marcar**: `UpdateItem` de la sucursal con `status = DELETING` (condición: existe y está activa).
   Desde ese instante, lo que crea ítems bajo la sucursal (agregar o renombrar producto) falla con
   404, porque sus transacciones exigen la sucursal activa. Para el cliente, una sucursal en
   eliminación no existe.
2. **Barrer**: consultar los ítems con prefijo `B#{branchId}#` y borrarlos en lotes, repitiendo hasta
   vaciar (máximo 5 barridos; si no alcanza, 409 y el cliente reintenta).
3. **Borrar** la sucursal, con la condición `status = DELETING`.

Es idempotente: si el proceso se interrumpe, repetir el `DELETE` retoma el barrido.

## Consecuencias

- Lo que se confirmó antes de la marca se elimina con la sucursal; lo que llega después recibe 404.
  Verificado eliminando una sucursal en medio de altas, cambios de stock y renombres simultáneos:
  no quedan ítems huérfanos.
- El top-stock excluye las sucursales en eliminación.
