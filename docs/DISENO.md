# Diseño

Este documento resume el diseño del proyecto y los criterios detrás de cada decisión. Las decisiones
principales tienen su registro corto (ADR) en [`decisiones/`](decisiones), con las alternativas
evaluadas y sus consecuencias. Para correr o desplegar el proyecto, el punto de partida es el
[README](../README.md).

## Criterios

El enunciado pide una API para franquicias, sucursales y productos con persistencia en la nube, y
valora la programación reactiva, los contenedores, la infraestructura como código y la calidad.
Tres criterios guían el diseño:

1. **El negocio no depende de la tecnología**: las reglas se prueban sin Spring, sin HTTP y sin base de datos.
2. **Correcto bajo concurrencia**: la API puede correr en varias instancias y recibir peticiones
   simultáneas sobre los mismos datos, así que nada puede depender de "leer, validar y escribir".
3. **Todo se puede reproducir**: cualquier persona puede correrlo, probarlo y desplegarlo con los
   pasos del README.

## Arquitectura

El stack es reactivo de punta a punta, con WebFlux y DynamoDB con el SDK asíncrono, sobre una
arquitectura limpia inspirada en el scaffold de Bancolombia, en un solo módulo organizado en `app`,
`core`, `integrations` y `modules`, con las reglas verificadas por ArchUnit
([ADR-001](decisiones/ADR-001-stack-y-arquitectura.md)).

## Modelo de datos y consistencia

Como todos los accesos parten de una franquicia, se usa una tabla única donde cada sucursal y cada
producto es un ítem independiente bajo la misma clave de partición; así, el producto con más stock
de cada sucursal sale de una sola consulta. La unicidad del nombre de producto se resuelve con un
ítem de reserva ([ADR-002](decisiones/ADR-002-modelo-de-datos.md)).

DynamoDB no tiene claves foráneas, bloqueos ni unicidad, así que la consistencia se construye con sus
propias herramientas: transacciones que verifican el padre, escrituras condicionadas, lecturas
consistentes y reintentos cuando otra petición gana
([ADR-003](decisiones/ADR-003-consistencia.md)).

## Funcionalidades con decisiones propias

- **Modificar el stock** con un valor absoluto e idempotente; la alternativa por delta queda
  documentada como evolución posible ([ADR-004](decisiones/ADR-004-modificacion-del-stock.md)).
- **Eliminar una sucursal**, adicional al enunciado: una lápida en la tabla y un barrido idempotente
  garantizan que no queden productos huérfanos aunque lleguen peticiones mientras se borra
  ([ADR-005](decisiones/ADR-005-eliminar-sucursal.md)).

## Calidad

Las garantías de concurrencia están cubiertas por pruebas automáticas, con hasta 50 peticiones
simultáneas y un verificador de integridad referencial. El entorno local (dos DynamoDB Local, un
inspector y scripts) permite correr todo con Docker como único requisito
([ADR-006](decisiones/ADR-006-calidad-y-pruebas.md)).

## Producción

- **Infraestructura** con Terraform y ECS Express Mode, donde AWS administra el balanceador y el
  HTTPS, con pocos recursos y mínimo privilegio ([ADR-007](decisiones/ADR-007-infraestructura-en-aws.md)).
- **CI/CD** con GitHub Actions y acceso a AWS por OIDC, sin llaves guardadas
  ([ADR-008](decisiones/ADR-008-ci-cd.md)).
- **Migraciones de datos** versionadas, aplicadas al arrancar con un lock distribuido
  ([ADR-009](decisiones/ADR-009-migraciones-de-datos.md)).

## Herramientas

El proyecto se desarrolló con apoyo de herramientas de IA; cómo se usaron, qué decidió el autor y qué
controles se aplicaron está en [este registro](decisiones/ADR-uso-de-herramientas-de-ia.md).

## Índice de decisiones

| ADR | Decisión |
|---|---|
| [001](decisiones/ADR-001-stack-y-arquitectura.md) | Stack (WebFlux + DynamoDB) y arquitectura limpia en un solo módulo |
| [002](decisiones/ADR-002-modelo-de-datos.md) | Modelo de datos: tabla única, UUID, reserva de nombre y top-stock |
| [003](decisiones/ADR-003-consistencia.md) | Consistencia bajo concurrencia |
| [004](decisiones/ADR-004-modificacion-del-stock.md) | El stock se modifica con un valor absoluto |
| [005](decisiones/ADR-005-eliminar-sucursal.md) | Eliminar una sucursal con lápida y barrido |
| [006](decisiones/ADR-006-calidad-y-pruebas.md) | Calidad, pruebas y entorno local |
| [007](decisiones/ADR-007-infraestructura-en-aws.md) | Infraestructura simple con Docker, Terraform y ECS Express Mode |
| [008](decisiones/ADR-008-ci-cd.md) | CI/CD con GitHub Actions y acceso a AWS por OIDC |
| [009](decisiones/ADR-009-migraciones-de-datos.md) | Migraciones de datos versionadas al arrancar |
