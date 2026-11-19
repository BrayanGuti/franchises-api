# ADR-003: Consistencia bajo concurrencia

**Estado**: aceptada

## Contexto

DynamoDB no tiene claves foráneas ni bloqueos, y la API puede correr en varias instancias. Sin
cuidado aparecen productos en sucursales que no existen, cambios que se pisan entre peticiones
simultáneas o lecturas que no reflejan lo recién escrito.

## Opciones evaluadas

| Opción | A favor | En contra |
|---|---|---|
| Leer, validar en memoria y escribir | Simple | Entre la lectura y la escritura otra petición puede cambiar los datos |
| Bloqueos distribuidos por recurso | Serializa todo | Lento, complejo y con riesgo de bloqueos huérfanos |
| **Escrituras condicionadas y transacciones de DynamoDB** | Atómico en el servidor, sin bloqueos | Hay que diseñar cada condición |

## Decisión

- **Sin huérfanos**: crear una sucursal o un producto es una transacción con un `ConditionCheck` del
  padre; si no existe, no se escribe nada (404).
- **Sin duplicados ni resurrecciones**: `attribute_not_exists` al crear y `attribute_exists` al
  modificar (un cambio de stock no recrea un producto borrado).
- **Leer y luego escribir** (eliminar y renombrar producto): la escritura se condiciona a lo leído y
  se reintenta hasta 3 veces; si otra petición gana siempre, 409 de conflicto.
- **Renombrar sin pisar el stock**: `UpdateItem` parcial que solo hace `SET name`. En productos, una
  transacción mueve la reserva (borra la vieja si es suya y crea la nueva con `attribute_not_exists`)
  y cambia el nombre condicionado al que se leyó; si solo cambian mayúsculas o tildes, la reserva no
  se mueve.
- **Leer lo último escrito**: `ConsistentRead` en las lecturas que lo necesitan.

## Consecuencias

- Verificado con pruebas automáticas de hasta 50 peticiones HTTP simultáneas y un verificador de
  integridad referencial ([ADR-006](ADR-006-calidad-y-pruebas.md)).
- DynamoDB Local siempre es fuertemente consistente, así que la consistencia eventual no se puede
  reproducir en local: por eso `ConsistentRead` es explícito.
- Las transacciones cuestan el doble de capacidad que una escritura simple; aceptable con este volumen.
