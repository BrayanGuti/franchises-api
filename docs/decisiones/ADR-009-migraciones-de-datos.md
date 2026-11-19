# ADR-009: Migraciones de datos versionadas al arrancar

**Estado**: aceptada

## Contexto

DynamoDB no tiene esquema, pero los datos sí tienen formato: un atributo nuevo que hay que completar
en los ítems existentes necesita un proceso controlado y repetible. La estructura de la tabla
(claves, índices, TTL) ya la define Terraform. Hoy hay una sola migración, la línea base, como el
`baseline` de Flyway: representa el modelo de datos actual.

## Opciones evaluadas

| Opción | A favor | En contra |
|---|---|---|
| Scripts manuales | Nada que construir | Sin historial ni repetibilidad |
| Tarea única de ECS en el CD que migra y termina | Separa migrar de servir | Definición de tarea, red y permisos adicionales, y un despliegue en dos pasos |
| **Al arrancar la aplicación, con lock distribuido** | Sin infraestructura nueva; imposible desplegar código sin sus migraciones | Una migración lenta alarga el arranque |

## Decisión

Un runner al estilo de Flyway en `integrations/dynamodb/migration`, sin negocio:

- Cada migración es una clase `V<3 dígitos>__<Descripcion>`; la primera, `V001__Baseline`, no
  transforma nada.
- **Historial en la propia tabla** (`PK = SCHEMA#MIGRATIONS`): solo se aplican las pendientes, en
  orden, y cada una se registra apenas termina.
- **Lock distribuido** con `PutItem` condicional y vencimiento: con varias instancias arrancando, una
  sola migra; si muere con el lock tomado, vence y otra retoma.
- La instancia **no se marca lista** hasta terminar, así que no recibe tráfico con los datos a medias;
  si una migración falla, no arranca y ECS vuelve a la versión anterior.
- `/actuator/info` publica la versión verificada y el CD la compara con la última del repositorio.

## Consecuencias

- Las migraciones deben ser **idempotentes** y seguir **expand/contract**, porque durante un
  despliegue conviven la versión vieja y la nueva de la app.
- Verificado con la aplicación real: dos instancias arrancando a la vez, una aplica y la otra
  encuentra la versión aplicada; con el lock tomado por otra, la instancia responde no lista (503)
  hasta que el lock vence.
