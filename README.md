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
