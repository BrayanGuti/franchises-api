# ADR: Uso de herramientas de IA en el desarrollo

**Estado**: aceptada · **Alcance**: todo el proyecto

## Contexto

El proyecto se desarrolló con apoyo de un asistente de programación con IA (Claude Code, de
Anthropic). Este documento explica con transparencia cómo se usó, qué decidió
el autor y qué controles se aplicaron, para que quien evalúe el proyecto pueda juzgarlo con esa
información.

## Cómo se trabajó

Para cada cambio:

1. **El autor lo aprobaba** antes de empezar y tomaba las decisiones de diseño, a menudo
   eligiendo entre alternativas que el asistente presentaba con sus ventajas y desventajas.
2. **El asistente implementaba**: código, pruebas, configuración y documentación, y ejecutaba las
   verificaciones (`./mvnw verify`, pruebas contra la aplicación real, `terraform plan`).
3. **El autor revisaba** el resultado en una ronda de preguntas para medir el código, armaba el
   stage.

## Decisiones del autor

Entre las decisiones que definieron el proyecto y que tomó el autor:

- La organización en `app`, `core`, `integrations` y `modules`, inspirada en otro proyecto propio,
  y un controlador por acción con sus DTOs ([ADR-001](ADR-001-stack-y-arquitectura.md)).
- La unicidad del nombre de producto y el comportamiento del top-stock con empates y sucursales vacías.
- **Eliminar una sucursal completando primero las peticiones en curso**: la idea de partida es del
  autor; el asistente la llevó a la lápida en la tabla para que funcionara con varias instancias
  ([ADR-005](ADR-005-eliminar-sucursal.md)).
- El stock como valor absoluto y la documentación de la alternativa por delta
  ([ADR-004](ADR-004-modificacion-del-stock.md)).
- Mantener la infraestructura simple, con AWS administrando el balanceador y el HTTPS
  ([ADR-007](ADR-007-infraestructura-en-aws.md)).
- Migrar al arrancar en lugar de con una tarea de ECS ([ADR-009](ADR-009-migraciones-de-datos.md)),
  y los scripts con Docker por defecto y datos de ejemplo ([ADR-006](ADR-006-calidad-y-pruebas.md)).

## Controles de calidad

Para no aceptar código sin entenderlo ni verificarlo:

- Cada cambio se verificó antes de incorporarse: build completo, cobertura mínima del 80 % (el build
  falla si no se cumple), reglas de arquitectura con ArchUnit y, si había endpoints, pruebas contra
  la aplicación real.
- Ningún archivo de código o de pruebas supera las 200 líneas, para que todo se pueda revisar.
- El comportamiento bajo concurrencia se demostró con pruebas automáticas estables, no con
  afirmaciones ([ADR-006](ADR-006-calidad-y-pruebas.md)).
- Los errores encontrados en la revisión o en el despliegue se corrigieron en commits propios, a la
  vista en el historial.

## Consecuencias

- La autoría y la responsabilidad del código son del autor: cada cambio lo revisó, lo entendió y lo
  incorporó él.
- El asistente aceleró la implementación, las pruebas y la documentación, y amplió las alternativas
  consideradas en cada decisión.
- Este documento es el único lugar del repositorio que describe el uso de estas herramientas.
