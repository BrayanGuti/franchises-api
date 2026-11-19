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
- Docker y Docker Compose

## Arquitectura

Arquitectura limpia inspirada en el
[Scaffold Clean Architecture de Bancolombia](https://github.com/bancolombia/scaffold-clean-architecture),
en un solo módulo Maven. El primer nivel separa por tipo de responsabilidad, para que el negocio
nunca se mezcle con la tecnología:

```
com.brayangutierrez.franchisesapi
├── app                  Arranque y composición (registro de casos de uso como beans)
├── core                 Transversal y técnico, sin negocio
│   ├── domain           Mecanismo de errores de negocio (BusinessException, BusinessErrorCode)
│   ├── web              Manejo de errores HTTP (ProblemDetail) y validaciones comunes
│   └── config           Swagger / OpenAPI
├── integrations         Tecnología externa, sin negocio
│   └── dynamodb         Cliente, creación de tabla y piezas comunes de persistencia
└── modules              Negocio
    └── franchises       Franquicias, sucursales y productos
        ├── domain
        │   ├── model    Entidades, gateways (interfaces de repositorio) y catálogo de errores
        │   └── usecase  Un caso de uso por acción
        └── infrastructure
            ├── persistence  Implementación de los gateways sobre DynamoDB
            └── web          Un controlador por acción, con sus DTOs
```

Reglas verificadas en cada build con ArchUnit (`ArchitectureTest`):

- El dominio de un módulo solo conoce Java, Reactor y el dominio de `core`: nada de Spring, AWS ni HTTP.
- `core` e `integrations` no conocen ningún módulo de negocio.
- La capa web de un módulo no usa su persistencia directamente: siempre pasa por los casos de uso.
- Los módulos no dependen entre sí y nadie depende de `app`.

## Modelo de datos

Una sola tabla de DynamoDB (`franchises`, modo on-demand) con clave compuesta `PK` + `SK`.
Toda la información de una franquicia comparte la misma `PK`, así que una sola consulta
trae sus sucursales y productos:

| Entidad | PK | SK | Atributos |
|---|---|---|---|
| Franquicia | `F#{franchiseId}` | `META` | `name` |
| Sucursal | `F#{franchiseId}` | `B#{branchId}` | `name` |
| Producto | `F#{franchiseId}` | `B#{branchId}#P#{productId}` | `name`, `stock` |
| Reserva de nombre | `F#{franchiseId}` | `B#{branchId}#NAME#{nombre normalizado}` | `productId` |

Los productos son ítems independientes, no una lista dentro de la sucursal: así cada producto
se modifica sin reescribir la sucursal y no hay riesgo de llegar al límite de 400 KB por ítem.

DynamoDB no tiene restricciones de unicidad. Para que una sucursal no tenga dos productos con el
mismo nombre, cada producto tiene una **reserva de nombre**: se crea en la misma transacción que el
producto con `attribute_not_exists`, así que si dos peticiones intentan el mismo nombre a la vez
solo una gana y la otra recibe 409.

**Eliminar una sucursal** sin dejar productos huérfanos, aunque lleguen peticiones mientras tanto:
primero se marca la sucursal con `status = DELETING` (desde ese instante, agregar o renombrar
productos responde 404), luego se borran todos sus productos y reservas, y al final la sucursal.
Lo que se confirmó antes de la marca se elimina con ella. Si el proceso se interrumpe, repetir el
`DELETE` lo completa. La marca vive en DynamoDB, así que funciona con varias instancias de la API.

En los perfiles `dev` y `test` la aplicación crea la tabla al arrancar si no existe.
En AWS la tabla la crea Terraform.

## Requisitos

- JDK 21
- Docker y Docker Compose

## Bases de datos locales

`docker-compose.yml` levanta dos instancias de DynamoDB Local, una por entorno, y un inspector
web para la base de desarrollo:

| Servicio | URL | Para qué | Datos |
|---|---|---|---|
| `dynamodb-dev` | `127.0.0.1:8000` | Perfil `dev` (por defecto) | Persistentes en el volumen `dynamodb-dev-data` |
| `dynamodb-test` | `127.0.0.1:8001` | Perfil `test` | En memoria, se pierden al detener el contenedor |
| `dynamodb-admin` | <http://127.0.0.1:8002> | Ver tablas e ítems de `dynamodb-dev` (claves `PK`/`SK`) | Solo lee `dynamodb-dev` |

```bash
docker compose up -d                                   # ambas bases
docker compose up -d dynamodb-dev                      # solo desarrollo
docker compose up -d dynamodb-test                     # solo pruebas
docker compose --profile tools up -d dynamodb-admin    # inspector (arranca también dynamodb-dev)
docker compose --profile tools down                    # detener todo (conserva los datos de dev)
docker compose --profile tools down -v                 # detener todo y borrar los datos de dev
```

- Las pruebas de integración se ejecutan siempre con el perfil `test`, por lo que nunca tocan la base de desarrollo.
- `dynamodb-admin` pertenece al perfil `tools`: no arranca con un `docker compose up` simple. Apunta
  solo a `dynamodb-dev` y nunca a AWS.
- Todos los puertos se publican solo en `127.0.0.1`: DynamoDB Local acepta cualquier credencial y
  el inspector no tiene autenticación, así que no quedan expuestos a la red.

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

### Pruebas de concurrencia

Las pruebas en `modules/franchises/concurrency` lanzan peticiones HTTP realmente simultáneas
(hasta 50 en vuelo) contra la aplicación y verifican invariantes, no un orden concreto:

| Escenario | Qué se verifica |
|---|---|
| 100 cambios de stock al mismo producto | Ninguno falla; el stock final es uno de los enviados; nombre y reserva intactos |
| Cambios de stock simultáneos en 10 productos | Cada producto termina con un valor de su propio rango |
| Renombrar mientras cambia el stock | Se conservan los dos cambios y la reserva del nombre se mueve |
| 20 altas idénticas | Exactamente 1 × 201 y 19 × 409; un solo producto y una sola reserva |
| Dos productos renombrados al mismo nombre | Uno gana (200) y el otro recibe 409 |
| 10 eliminaciones del mismo producto | Exactamente 1 × 204 y 9 × 404; no queda nada |
| Eliminar una sucursal mientras llegan altas, stock y renombres | Cada petición se completa o recibe 404; no quedan ítems huérfanos |
| Eliminaciones simultáneas de la misma sucursal | Exactamente un 204 |

### Pruebas de consistencia de datos

Verifican que bajo concurrencia nunca queden datos inconsistentes:

| Escenario | Qué se verifica |
|---|---|
| Eliminar productos mientras llegan cambios de stock y de nombre | No queda ningún producto fantasma ni reserva huérfana |
| Carga mezclada simultánea (altas, renombres, stock y bajas que compiten por los mismos nombres) | **Integridad referencial**: cada producto tiene exactamente su reserva, cada reserva apunta a un producto existente y no hay nombres duplicados |
| Altas simultáneas en una sucursal inexistente | Todas responden 404 y no se escribe nada |
| Escribir y leer de inmediato, 20 veces seguidas | Cada lectura refleja la escritura anterior (lectura fuertemente consistente) |
| Renombrar y eliminar | Los nombres liberados se pueden volver a usar |

El verificador de integridad tiene sus propias pruebas que rompen los datos a propósito, para
garantizar que detecta las inconsistencias.

Para ejecutar las pruebas de concurrencia y consistencia solas:

```bash
./mvnw verify -Dtest=ArchitectureTest -Dit.test='*ConcurrencyIT,*ConsistencyIT' -Djacoco.haltOnFailure=false
```

> DynamoDB Local siempre responde con consistencia fuerte, así que la consistencia eventual de las
> lecturas por defecto de DynamoDB no se puede reproducir en local. Por eso las lecturas que deben
> reflejar lo último escrito usan `ConsistentRead` explícitamente.

## Integración continua

Cada push y pull request a `main` ejecuta [`.github/workflows/ci.yml`](.github/workflows/ci.yml):
levanta `dynamodb-test`, corre `./mvnw verify` (pruebas y umbral de cobertura del 80 %),
publica los reportes como artefacto y envía la cobertura a Coveralls. En paralelo construye la
imagen Docker y comprueba que no corra como root.

## Ejecutar la aplicación

### Opción A: todo con Docker (no requiere Java instalado)

Levanta las bases, el inspector y la API empaquetada en su imagen:

```bash
docker compose --profile app up -d --build
```

```bash
docker compose --profile app down        # detener todo (conserva los datos de dev)
```

### Opción B: la API con Maven (para desarrollar)

Con el perfil `dev` (por defecto), contra `dynamodb-dev`, con el inspector para ver los datos:

```bash
docker compose --profile tools up -d dynamodb-admin    # levanta dynamodb-dev + el inspector
./mvnw spring-boot:run
```

En ambos casos:

- Estado de la aplicación: <http://localhost:8080/actuator/health>
- Swagger: <http://localhost:8080/swagger-ui.html>
- Inspector de la base: <http://127.0.0.1:8002>

### La imagen Docker

[`Dockerfile`](Dockerfile) de dos etapas:

- **Compilación** con `maven:3.9.16-eclipse-temurin-21`: las dependencias se descargan en una capa
  propia y con caché de `~/.m2`, así que un cambio de código no vuelve a descargarlas.
- **Ejecución** con `eclipse-temurin:21.0.13_9-jre-noble` (solo JRE): la aplicación se separa en
  capas de Spring Boot (dependencias, cargador y código) para que un cambio de código solo
  reemplace la última.
- Corre con un usuario sin privilegios (`app`), nunca como root.
- `HEALTHCHECK` sobre `/actuator/health/readiness`: el contenedor queda `healthy` solo cuando la
  aplicación terminó de arrancar.
- Las pruebas no se ejecutan dentro del build de la imagen (necesitan `dynamodb-test`); las corre el CI.

## Infraestructura en AWS (Terraform)

La infraestructura está definida como código en [`infra/`](infra), en una sola carpeta:

| Archivo | Contenido |
|---|---|
| `main.tf` | Tabla de DynamoDB, repositorio de ECR y el servicio en **ECS Express Mode** |
| `iam.tf` | Los tres roles IAM de mínimo privilegio |
| `variables.tf`, `outputs.tf`, `versions.tf` | Parámetros, salidas (la URL pública) y versiones; estado remoto en S3 |

**ECS Express Mode** ejecuta la imagen en Fargate y AWS administra por nosotros el balanceador de
carga, los grupos de seguridad y el certificado HTTPS, en la VPC por defecto de la cuenta. Entrega
una URL pública estable con HTTPS (`https://<servicio>.ecs.<región>.on.aws`) y despliegues
graduales con vuelta atrás automática si la versión nueva falla.

Recursos:

- **DynamoDB**: tabla única `PK`/`SK`, on-demand, cifrada y con recuperación a un punto en el tiempo.
- **ECR**: tags inmutables (cada versión es el SHA de un commit), escaneo de vulnerabilidades y solo
  las últimas 10 imágenes.
- **IAM de mínimo privilegio**:
  - Rol de la tarea (la aplicación): solo `GetItem`, `PutItem`, `UpdateItem`, `DeleteItem`, `Query` y
    `ConditionCheckItem`, y solo sobre su tabla. No puede crear ni borrar tablas.
  - Rol de ejecución: solo descarga la imagen de su repositorio y escribe en su grupo de logs.
  - Rol de infraestructura: el que usa ECS Express para administrar el balanceador y la red.
- Sin llaves en el código: en AWS la aplicación usa las credenciales temporales del rol de la tarea.

### Desplegar desde un entorno local

Requisitos: Terraform 1.10 o superior, Docker y la CLI de AWS con credenciales configuradas.
La cuenta debe tener la VPC por defecto en la región (viene creada en las cuentas de AWS).

```bash
# 1. Bucket para el estado de Terraform (una sola vez)
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
BUCKET="franchises-api-tfstate-$ACCOUNT"
aws s3api create-bucket --bucket "$BUCKET" --region us-east-1
aws s3api put-bucket-versioning --bucket "$BUCKET" --versioning-configuration Status=Enabled

# 2. Inicializar Terraform con ese bucket
terraform -chdir=infra init -backend-config="bucket=$BUCKET" -backend-config="region=us-east-1"

# 3. Crear el repositorio de imágenes y subir la imagen
terraform -chdir=infra apply -target=aws_ecr_repository.this -var image_tag=v1
REPO=$(terraform -chdir=infra output -raw ecr_repository_url)
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin "${REPO%/*}"
docker build -t "$REPO:v1" . && docker push "$REPO:v1"

# 4. Crear el resto de la infraestructura con esa imagen
terraform -chdir=infra apply -var image_tag=v1
terraform -chdir=infra output api_url
```

El estado vive en S3 para que el CD (GitHub Actions) y una máquina local trabajen sobre el mismo
estado; cada ejecución del CI empieza en una máquina vacía y, sin estado compartido, Terraform no
sabría qué recursos ya existen.

### Costo y cómo destruir todo

Costo aproximado en `us-east-1` con una tarea encendida todo el mes: balanceador de carga ~16 USD,
Fargate (0,25 vCPU y 0,5 GB) ~9 USD y direcciones IPv4 públicas ~4 USD, unos **30 USD al mes**.
ECS Express Mode no tiene cargo adicional; DynamoDB on-demand, ECR y los logs cuestan centavos con
el tráfico de una prueba.

Para no seguir pagando, al terminar:

```bash
terraform -chdir=infra destroy -var image_tag=v1
```

El bucket del estado cuesta centavos; se puede conservar o borrar después de vaciarlo.

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| `POST` | `/api/franchises` | Agregar una franquicia | 201, 400 |
| `PATCH` | `/api/franchises/{franchiseId}` | Actualizar el nombre de una franquicia | 200, 400, 404 |
| `POST` | `/api/franchises/{franchiseId}/branches` | Agregar una sucursal a una franquicia | 201, 400, 404 |
| `PATCH` | `/api/franchises/{franchiseId}/branches/{branchId}` | Actualizar el nombre de una sucursal | 200, 400, 404 |
| `DELETE` | `/api/franchises/{franchiseId}/branches/{branchId}` | Eliminar una sucursal y sus productos | 204, 400, 404, 409 |
| `POST` | `/api/franchises/{franchiseId}/branches/{branchId}/products` | Agregar un producto a una sucursal | 201, 400, 404, 409 |
| `DELETE` | `/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}` | Eliminar un producto de una sucursal | 204, 400, 404, 409 |
| `PATCH` | `/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock` | Modificar el stock de un producto | 200, 400, 404 |
| `PATCH` | `/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}` | Actualizar el nombre de un producto | 200, 400, 404, 409 |
| `GET` | `/api/franchises/{franchiseId}/top-stock-products` | Producto con más stock por sucursal | 200, 400, 404 |

- Los identificadores son UUID generados por el sistema; un ID con otro formato responde 400.
- El nombre de un producto es único dentro de su sucursal, sin distinguir mayúsculas, tildes ni
  espacios sobrantes: "Café", "cafe" y "  CAFÉ " se consideran el mismo producto (409).
- Producto con más stock por sucursal: se lee con una sola consulta fuertemente consistente
  (refleja de inmediato cualquier cambio confirmado). Si hay empate se devuelven todos los
  productos empatados; las sucursales sin productos se omiten; el orden es por sucursal y luego por
  producto.

Ejemplo:

```bash
curl -X POST http://localhost:8080/api/franchises \
  -H "Content-Type: application/json" \
  -d '{"name":"Cafeterias del Norte"}'
```

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
| 409 | El recurso ya existe, o cambió mientras se procesaba la operación (se puede reintentar) |
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
