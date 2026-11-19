# ADR-001: Stack y arquitectura

**Estado**: aceptada

## Contexto

La prueba pide una API para franquicias, sucursales y productos con persistencia en la nube, y
valora la programación reactiva. El negocio debe poder probarse y cambiar sin depender de Spring,
de HTTP ni de la base de datos.

## Opciones evaluadas

| Tema | Opciones | Elegida |
|---|---|---|
| Stack | Spring MVC + base relacional · WebFlux + MongoDB · **WebFlux + DynamoDB** | Reactivo de punta a punta (Netty + SDK v2 asíncrono) y base administrada, sin servidores, emulable en local |
| Estructura | Multimódulo Maven (como el scaffold de Bancolombia) · **un módulo + ArchUnit** | Misma separación con menos complejidad; las reglas se verifican en cada build |
| Organización | Por entidad · **por responsabilidad** | Las entidades están muy acopladas (los productos viven dentro de la sucursal y esta dentro de la franquicia) |
| Punto de entrada | RouterFunction + Handler · **`@RestController`** | Validación, ProblemDetail y Swagger sin código extra; igual de reactivo |

## Decisión

- **Java 21, Spring Boot 4.1 con WebFlux, DynamoDB con el AWS SDK v2 asíncrono**, Maven con wrapper.
- Arquitectura limpia inspirada en el
  [scaffold de Bancolombia](https://github.com/bancolombia/scaffold-clean-architecture), en un solo
  módulo, con el primer nivel por responsabilidad:
  - `app`: arranque; registra como beans los casos de uso, que no tienen anotaciones de Spring.
  - `core`: transversal y técnico, sin negocio (errores, manejo HTTP, Swagger).
  - `integrations`: tecnología externa, sin negocio (cliente de DynamoDB, migraciones).
  - `modules/franchises`: el negocio (`domain` con modelo, gateways y casos de uso;
    `infrastructure` con persistencia y web).
- **ArchUnit** hace cumplir que el dominio solo conoce Java, Reactor y el dominio de `core`, que
  `core` e `integrations` no conocen módulos, que web y persistencia no se usan entre sí y que
  nadie depende de `app`.
- **Un controlador por acción**, cada uno en su carpeta con sus DTOs; una respuesta usada por dos o
  más acciones pasa a `<entidad>/dto/`.
- **Errores**: una sola `BusinessException` con un catálogo por módulo (`FranchiseError`) y
  categorías que `core` traduce a HTTP con **ProblemDetail (RFC 7807)**.
- Ningún archivo de código supera las 200 líneas;

## Consecuencias

- Los casos de uso se prueban sin contexto de Spring.
- Todo es no bloqueante; solo se bloquea al arrancar (crear la tabla en local, migrar).
- ArchUnit no ve las constantes `static final String` (el compilador las copia), así que las claves
  de DynamoDB se construyen con métodos (`DynamoKeys`).
