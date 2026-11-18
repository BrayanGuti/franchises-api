# Franchises API

[![CI](https://github.com/BrayanGuti/franchises-api/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/BrayanGuti/franchises-api/actions/workflows/ci.yml)
[![Coverage Status](https://coveralls.io/repos/github/BrayanGuti/franchises-api/badge.svg?branch=main)](https://coveralls.io/github/BrayanGuti/franchises-api?branch=main)

API reactiva para gestionar franquicias, sus sucursales y los productos de cada sucursal.
Construida con Spring Boot 4 y WebFlux, con persistencia en Amazon DynamoDB.

## Stack

- Java 21
- Spring Boot 4.1.2 (WebFlux, Validation, Actuator)
- AWS SDK v2 para DynamoDB
- Maven
- JaCoCo para cobertura de código

## Arquitectura

Arquitectura limpia por capas, inspirada en el
[Scaffold Clean Architecture de Bancolombia](https://github.com/bancolombia/scaffold-clean-architecture),
en un solo módulo Maven:

```
com.brayangutierrez.franchisesapi
├── domain
│   ├── model        Entidades, gateways (interfaces de repositorio) y errores de negocio
│   └── usecase      Casos de uso: la lógica de la aplicación
├── infrastructure
│   ├── drivenadapters/dynamodb     Implementación de los gateways con DynamoDB
│   └── entrypoints/reactiveweb     Controladores REST, DTOs y manejo de errores HTTP
└── config           Arranque: registro de casos de uso como beans
```

Las dependencias solo apuntan hacia el dominio: `domain` no conoce Spring, DynamoDB ni HTTP.
La regla se verifica en cada build con ArchUnit (`ArchitectureTest`).

## Modelo de datos

Una sola tabla de DynamoDB (`franchises`, modo on-demand) con clave compuesta `PK` + `SK`.
Toda la información de una franquicia comparte la misma `PK`, así que una sola consulta
trae sus sucursales y productos:

| Entidad | PK | SK | Atributos |
|---|---|---|---|
| Franquicia | `F#{franchiseId}` | `META` | `name` |
| Sucursal | `F#{franchiseId}` | `B#{branchId}` | `name` |
| Producto | `F#{franchiseId}` | `B#{branchId}#P#{productId}` | `name`, `stock` |

Los productos son ítems independientes, no una lista dentro de la sucursal: así cada producto
se modifica sin reescribir la sucursal y no hay riesgo de llegar al límite de 400 KB por ítem.

En los perfiles `dev` y `test` la aplicación crea la tabla al arrancar si no existe.
En AWS la tabla la crea Terraform.

## Requisitos

- JDK 21
- Docker y Docker Compose

## Bases de datos locales

`docker-compose.yml` levanta dos instancias de DynamoDB Local, una por entorno:

| Servicio | Puerto | Perfil Spring | Datos |
|---|---|---|---|
| `dynamodb-dev` | 8000 | `dev` (por defecto) | Persistentes en el volumen `dynamodb-dev-data` |
| `dynamodb-test` | 8001 | `test` | En memoria, se pierden al detener el contenedor |

```bash
docker compose up -d                  # ambas bases
docker compose up -d dynamodb-dev     # solo desarrollo
docker compose up -d dynamodb-test    # solo pruebas
docker compose down                   # detener (conserva los datos de dev)
docker compose down -v                # detener y borrar los datos de dev
```

Las pruebas de integración se ejecutan siempre con el perfil `test`, por lo que nunca tocan la base de desarrollo.

## Compilar y ejecutar pruebas

Requiere `dynamodb-test` en ejecución:

```bash
docker compose up -d dynamodb-test
./mvnw verify
```

En Windows:

```powershell
.\mvnw.cmd verify
```

El comando ejecuta:

- Pruebas unitarias (`*Test`) con Surefire.
- Pruebas de integración (`*IT`) con Failsafe.
- Reporte de cobertura unificado en `target/site/jacoco/index.html`.
- Verificación de cobertura mínima del 80 %: si no se cumple, el build falla.

## Integración continua

Cada push y pull request a `main` ejecuta [`.github/workflows/ci.yml`](.github/workflows/ci.yml):
levanta `dynamodb-test`, corre `./mvnw verify` (pruebas y umbral de cobertura del 80 %),
publica los reportes como artefacto y envía la cobertura a Coveralls.

## Ejecutar la aplicación

Con el perfil `dev` (por defecto), contra `dynamodb-dev`:

```bash
docker compose up -d dynamodb-dev
./mvnw spring-boot:run
```

Estado de la aplicación: <http://localhost:8080/actuator/health>

## Documentación de la API

Con la aplicación en ejecución:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Especificación OpenAPI (JSON): <http://localhost:8080/v3/api-docs>

## Formato de errores

Todos los errores siguen el estándar [ProblemDetail (RFC 7807)](https://www.rfc-editor.org/rfc/rfc7807)
con `Content-Type: application/problem+json`:

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos, JSON mal formado o parámetros con formato incorrecto |
| 404 | La franquicia, sucursal, producto o ruta no existe |
| 409 | El recurso ya existe |
| 500 | Error inesperado (sin exponer detalles internos) |

Ejemplo de error de validación:

```json
{
  "type": "about:blank",
  "title": "Solicitud inválida",
  "status": 400,
  "detail": "La solicitud contiene datos inválidos",
  "instance": "/api/franchises",
  "timestamp": "2026-01-01T12:00:00Z",
  "errors": {
    "name": "El nombre es obligatorio"
  }
}
```
