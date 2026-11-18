# Franchises API

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

## Compilar y ejecutar pruebas

```bash
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

## Ejecutar la aplicación

```bash
./mvnw spring-boot:run
```

Estado de la aplicación: <http://localhost:8080/actuator/health>
