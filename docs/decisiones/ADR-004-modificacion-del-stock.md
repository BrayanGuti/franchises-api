# ADR-004: El stock se modifica con un valor absoluto

**Estado**: aceptada

## Contexto

El enunciado pide "modificar el stock de un producto". Hay dos formas de interpretarlo: fijar la
cantidad disponible o registrar movimientos (entradas y salidas).

## Opciones evaluadas

| Opción | A favor | En contra |
|---|---|---|
| **Valor absoluto**: `PATCH .../stock` con `{"stock": 40}` | Lectura literal del enunciado; simple (el cliente envía el conteo real); **idempotente** (repetir la petición deja el mismo valor) | Con escrituras simultáneas gana la última |
| Movimiento por delta: `{"delta": -3}` con `SET stock = stock + :delta` y condición `stock + :delta >= 0` (409 si quedaría negativo) | Atómico; con varias fuentes vendiendo a la vez no se pierde ninguna venta | **No es idempotente**: un reintento por un timeout descuenta dos veces, salvo que se agregue un identificador de idempotencia |

## Decisión

Valor absoluto. Cada petición representa un conteo completo, así que "gana la última" es el
comportamiento esperado.

Implementación: `UpdateItem` con un ítem parcial (`IgnoreNullsMode.SCALAR_ONLY`), que genera solo
`SET stock` y por eso no pisa un renombrado simultáneo, con la condición `attribute_exists(PK)` para
no resucitar un producto borrado.

## Consecuencias

- Validado con 100 cambios de stock simultáneos al mismo producto: todos responden 200 y el valor
  final es uno de los enviados, con el nombre y la reserva intactos.
- El delta no se descartó por inviable, sino porque el enunciado no pide sumar ni restar y su
  complejidad (idempotencia) no aporta en este caso. **Evolución natural** si el negocio necesita
  registrar ventas concurrentes: un endpoint adicional `POST .../stock-adjustments` con identificador
  de idempotencia, sin quitar el valor absoluto.
