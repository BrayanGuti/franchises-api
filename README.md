# Franchises API

[![CI](https://github.com/BrayanGuti/franchises-api/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/BrayanGuti/franchises-api/actions/workflows/ci.yml)
[![CD](https://github.com/BrayanGuti/franchises-api/actions/workflows/cd.yml/badge.svg)](https://github.com/BrayanGuti/franchises-api/actions/workflows/cd.yml)
[![Coverage Status](https://coveralls.io/repos/github/BrayanGuti/franchises-api/badge.svg?branch=main)](https://coveralls.io/github/BrayanGuti/franchises-api?branch=main)

API reactiva para gestionar franquicias, sus sucursales y los productos de cada sucursal.
Construida con Java 21, Spring Boot 4 (WebFlux) y Amazon DynamoDB, empaquetada en Docker y
desplegada en AWS (ECS) con Terraform y GitHub Actions.

**Demo en línea** (disponible durante el proceso de evaluación):

- Swagger: <https://fr-b02799d1390b4ad7a026c503c6e2ebd3.ecs.us-east-1.on.aws/swagger-ui.html>
- API: `https://fr-b02799d1390b4ad7a026c503c6e2ebd3.ecs.us-east-1.on.aws/api/franchises`

## Contenido

1. [Inicio rápido](#inicio-rápido)
2. [Requisitos](#requisitos)
3. [Ejecutar en local](#ejecutar-en-local)
4. [Probar la API](#probar-la-api)
5. [Pruebas automáticas](#pruebas-automáticas)
6. [Desplegar en AWS desde un entorno local](#desplegar-en-aws-desde-un-entorno-local)
7. [Integración y despliegue continuos](#integración-y-despliegue-continuos)
8. [Endpoints](#endpoints)
9. [Arquitectura](#arquitectura)
10. [Modelo de datos](#modelo-de-datos)
11. [Estructura del repositorio](#estructura-del-repositorio)

## Inicio rápido

Solo necesitas **Docker** y **Git** (en Windows, usa la terminal **Git Bash**, que viene con Git):

```bash
git clone https://github.com/BrayanGuti/franchises-api.git
cd franchises-api
scripts/dev.sh
```

El script levanta la base de datos, un inspector web de la base y la API, carga datos de ejemplo
y, cuando todo está listo, abre en el navegador:

- **Swagger** para probar la API: <http://localhost:8080/swagger-ui.html>
- **Inspector de la base** (tablas e ítems): <http://127.0.0.1:8002>

La primera vez tarda unos minutos (descarga imágenes y compila la API dentro de Docker). Para
detener todo: `scripts/stop.sh`.

## Requisitos

| Para | Necesitas |
|---|---|
| Correr la API en local (`scripts/dev.sh`) | Docker con Docker Compose v2. Nada más: la API se compila dentro de su imagen |
| Correr la API sin Docker (`scripts/dev.sh --maven`) | Además, el **JDK 21** |
| Ejecutar las pruebas (`scripts/test.sh`) | Docker y el **JDK 21** |
| Desplegar en AWS | Docker, la [CLI de AWS](https://aws.amazon.com/cli/) con credenciales y, opcionalmente, Terraform 1.10 o superior (también se puede usar desde Docker) |

**Sobre el JDK**: Maven usa el JDK de `JAVA_HOME` y, si no está definido, el `java` del PATH. Si
tienes varios JDK instalados, `JAVA_HOME` debe apuntar al 21; si no, los scripts lo indican con el
JDK que encontraron:

```bash
java -version          # debe decir 21 o superior
echo "$JAVA_HOME"      # si está definido, debe ser la carpeta del JDK 21
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot"   # ejemplo en Git Bash
```

**En Windows**: los scripts son de Bash. Ejecútalos desde **Git Bash** (en VS Code: menú de la
terminal → Git Bash) o WSL. En PowerShell, el comando `bash` suele abrir WSL y no Git Bash; si
prefieres PowerShell, usa la ruta completa: `& "C:\Program Files\Git\bin\bash.exe" scripts/dev.sh`,
o los comandos manuales de abajo.

## Ejecutar en local

### Con los scripts (recomendado)

| Script | Qué hace |
|---|---|
| `scripts/dev.sh` | Levanta `dynamodb-dev`, el inspector y la API (en Docker) en perfil `dev`. Cuando la API está lista muestra y abre Swagger y el inspector, y deja los logs de la API en la terminal |
| `scripts/test.sh` | Levanta `dynamodb-test`, ejecuta todas las pruebas con el umbral de cobertura, muestra el resumen y abre el reporte |
| `scripts/stop.sh` | Detiene todos los contenedores; los datos de desarrollo se conservan |

```bash
scripts/dev.sh                 # todo en Docker; Ctrl+C deja de mostrar los logs (la API sigue corriendo)
scripts/dev.sh --maven         # la API con Maven, sin Docker para la API (requiere el JDK 21)
scripts/test.sh                # todas las pruebas + cobertura
scripts/test.sh -Dit.test='*ConcurrencyIT' -Djacoco.haltOnFailure=false   # argumentos extra para Maven
scripts/stop.sh                # detener todo
scripts/stop.sh --clean        # detener todo y borrar los datos de desarrollo (pide confirmación)
```

Todos aceptan `--help`, y `--no-browser` (o `NO_BROWSER=1`) evita abrir el navegador. Antes de
empezar comprueban que Docker esté corriendo y, si hace falta, que el JDK sea 21 o superior.

**Datos de ejemplo**: si la base de desarrollo es nueva (primera ejecución o después de
`scripts/stop.sh --clean`), `dev.sh` crea por la API dos franquicias con tres sucursales cada una;
cada sucursal tiene un producto estrella, con mucho más stock que los demás, que es el que devuelve
el top-stock. Al terminar muestra las URL del top-stock de cada franquicia. `--seed` los carga de
nuevo (con IDs nuevos, sin borrar los anteriores) y `--no-seed` lo evita.

### Sin los scripts (cualquier terminal, también PowerShell)

Todo en Docker:

```bash
docker compose --profile app up -d --build    # bases + inspector + API
docker compose --profile app logs -f app      # logs de la API
docker compose --profile app down             # detener (conserva los datos de desarrollo)
```

La API con Maven (requiere el JDK 21):

```bash
docker compose --profile tools up -d dynamodb-admin    # dynamodb-dev + el inspector
./mvnw spring-boot:run                                 # en PowerShell: .\mvnw.cmd spring-boot:run
```

### Servicios locales

| Servicio | Dirección | Para qué |
|---|---|---|
| API | <http://localhost:8080> | Estado en `/actuator/health`; versión de datos en `/actuator/info` |
| Swagger | <http://localhost:8080/swagger-ui.html> | Documentación interactiva; la especificación OpenAPI está en `/v3/api-docs` |
| `dynamodb-admin` | <http://127.0.0.1:8002> | Inspector web de `dynamodb-dev`: tablas, ítems y claves `PK`/`SK` |
| `dynamodb-dev` | `127.0.0.1:8000` | Base del perfil `dev`; datos persistentes en el volumen `dynamodb-dev-data` |
| `dynamodb-test` | `127.0.0.1:8001` | Base de las pruebas; en memoria |

- `dynamodb-dev` y `dynamodb-test` son DynamoDB Local: hablan la API de DynamoDB, no son páginas
  web (abrirlas en el navegador responde 400). Para ver los datos usa el inspector.
- Las pruebas usan siempre el perfil `test` y `dynamodb-test`, así que nunca tocan la base de desarrollo.
- Todos los puertos se publican solo en `127.0.0.1`: DynamoDB Local acepta cualquier credencial y
  el inspector no tiene autenticación, así que no quedan expuestos a la red.

## Probar la API

La forma más simple es Swagger: <http://localhost:8080/swagger-ui.html> (o el de la demo en línea).
Con los datos de ejemplo, `dev.sh` muestra al final las URL del top-stock de cada franquicia.

Con `curl`, el flujo completo (reemplaza los IDs por los que devuelve cada respuesta):

```bash
API=http://localhost:8080/api

# 1. Crear una franquicia → 201 {"id":"<franchiseId>","name":"Cafe Andino"}
curl -s -X POST $API/franchises -H 'Content-Type: application/json' -d '{"name":"Cafe Andino"}'

# 2. Agregarle una sucursal → 201 {"id":"<branchId>",...}
curl -s -X POST $API/franchises/<franchiseId>/branches -H 'Content-Type: application/json' -d '{"name":"Centro"}'

# 3. Agregar productos a la sucursal → 201 {"id":"<productId>",...}
curl -s -X POST $API/franchises/<franchiseId>/branches/<branchId>/products \
  -H 'Content-Type: application/json' -d '{"name":"Cafe molido","stock":80}'

# 4. Modificar el stock (valor absoluto) → 200
curl -s -X PATCH $API/franchises/<franchiseId>/branches/<branchId>/products/<productId>/stock \
  -H 'Content-Type: application/json' -d '{"stock":95}'

# 5. Producto con más stock de cada sucursal de la franquicia → 200 [...]
curl -s $API/franchises/<franchiseId>/top-stock-products
```

> En Windows, `curl` desde Git Bash puede enviar mal las tildes escritas en la línea de comandos;
> Swagger no tiene ese problema.

## Pruebas automáticas

```bash
scripts/test.sh
```

o a mano, con `dynamodb-test` levantada:

```bash
docker compose up -d dynamodb-test
./mvnw verify                     # en PowerShell: .\mvnw.cmd verify
```

- Pruebas unitarias (`*Test`) con Surefire y de integración (`*IT`) con Failsafe, contra DynamoDB Local.
- Reporte de cobertura unificado en `target/site/jacoco/index.html`.
- Cobertura mínima del 80 % (JaCoCo rompe el build si no se cumple); la actual supera el 95 %.
- `ArchitectureTest` (ArchUnit) verifica en cada build las reglas de la arquitectura.

### Concurrencia

Las pruebas en `modules/franchises/concurrency` lanzan peticiones HTTP realmente simultáneas
(hasta 50 en vuelo) y verifican invariantes, no un orden concreto:

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

### Consistencia de datos

| Escenario | Qué se verifica |
|---|---|
| Eliminar productos mientras llegan cambios de stock y de nombre | No queda ningún producto fantasma ni reserva huérfana |
| Carga mezclada simultánea (altas, renombres, stock y bajas que compiten por los mismos nombres) | **Integridad referencial**: cada producto tiene exactamente su reserva, cada reserva apunta a un producto existente y no hay nombres duplicados |
| Altas simultáneas en una sucursal inexistente | Todas responden 404 y no se escribe nada |
| Escribir y leer de inmediato, 20 veces seguidas | Cada lectura refleja la escritura anterior (lectura fuertemente consistente) |
| Renombrar y eliminar | Los nombres liberados se pueden volver a usar |

El verificador de integridad tiene sus propias pruebas, que rompen los datos a propósito para
garantizar que detecta las inconsistencias. Para correr solo estas pruebas:

```bash
scripts/test.sh -Dtest=ArchitectureTest -Dit.test='*ConcurrencyIT,*ConsistencyIT' -Djacoco.haltOnFailure=false
```

> DynamoDB Local siempre responde con consistencia fuerte, así que la consistencia eventual de las
> lecturas por defecto de DynamoDB no se puede reproducir en local. Por eso las lecturas que deben
> reflejar lo último escrito usan `ConsistentRead` explícitamente.

## Desplegar en AWS desde un entorno local

La infraestructura está definida con Terraform en [`infra/`](infra). Un despliegue completo crea:

- **DynamoDB**: la tabla, on-demand, cifrada y con recuperación a un punto en el tiempo.
- **ECR**: repositorio de imágenes con tags inmutables, escaneo de vulnerabilidades y solo las últimas 10.
- **ECS Express Mode**: ejecuta la imagen en Fargate y AWS administra el balanceador de carga, los
  grupos de seguridad y el certificado HTTPS. Entrega una URL pública estable
  (`https://<servicio>.ecs.<región>.on.aws`) y despliega cada versión de forma gradual, volviendo
  sola a la anterior si la nueva falla.
- **IAM de mínimo privilegio**: la aplicación solo puede leer y escribir ítems de su tabla (no crear
  ni borrar tablas), y no hay llaves en el código: usa las credenciales temporales de su rol.
- **Acceso de GitHub Actions por OIDC** (para el despliegue continuo, ver la sección siguiente).

### 1. Preparar el entorno

```bash
# Credenciales de AWS: con "aws configure" (o SSO). Si usas un perfil con nombre:
export AWS_PROFILE=<tu-perfil>
aws sts get-caller-identity          # debe mostrar tu cuenta

export AWS_REGION=us-east-1
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
BUCKET="franchises-api-tfstate-$ACCOUNT"
TAG=$(git rev-parse HEAD)            # cada versión de la imagen es el SHA de un commit
```

La cuenta debe tener la **VPC por defecto** en la región (viene creada en las cuentas de AWS;
ECS Express la usa). Se comprueba con
`aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[].VpcId'`.

Define `tf` según cómo vayas a usar Terraform:

```bash
# Con Terraform instalado:
tf() { terraform -chdir=infra "$@"; }

# O sin instalarlo, con Docker (funciona igual en Linux, macOS y Git Bash):
tf() {
  MSYS_NO_PATHCONV=1 docker run --rm -i -e AWS_PROFILE -e AWS_REGION \
    -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_SESSION_TOKEN \
    -v "$(pwd -W 2>/dev/null || pwd)/infra:/infra" \
    -v "$(cd ~/.aws && (pwd -W 2>/dev/null || pwd)):/root/.aws:ro" \
    -w /infra hashicorp/terraform:1.17.1 "$@"
}
```

### 2. Crear el bucket del estado de Terraform (una sola vez)

El estado vive en S3 para que tu máquina y el CD (GitHub Actions) trabajen sobre el mismo estado:
cada ejecución del CI empieza en una máquina vacía y, sin estado compartido, Terraform no sabría
qué recursos ya existen.

```bash
aws s3api create-bucket --bucket "$BUCKET" --region us-east-1
aws s3api put-bucket-versioning --bucket "$BUCKET" --versioning-configuration Status=Enabled
aws s3api put-public-access-block --bucket "$BUCKET" --public-access-block-configuration \
  BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

tf init -backend-config="bucket=$BUCKET" -backend-config="region=us-east-1"
```

### 3. Crear el repositorio de imágenes y subir la imagen

El servicio necesita que la imagen exista antes de crearse, así que primero va solo el repositorio:

```bash
tf apply -target=aws_ecr_repository.this -target=aws_ecr_lifecycle_policy.this -var image_tag=$TAG

REPO=$(tf output -raw ecr_repository_url)
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin "${REPO%/*}"
docker build --platform linux/amd64 -t "$REPO:$TAG" .
docker push "$REPO:$TAG"
```

`--platform linux/amd64` es la arquitectura del servicio en Fargate; en una Mac con Apple Silicon
es imprescindible.

### 4. Crear el resto de la infraestructura

```bash
tf apply -var image_tag=$TAG          # muestra el plan y pide confirmación ("yes"); tarda ~10 minutos
tf output                             # api_url y swagger_url
```

### 5. Comprobar el despliegue

```bash
API_URL=$(tf output -raw api_url)
curl "$API_URL/actuator/health/readiness"     # {"status":"UP"}
curl "$API_URL/actuator/info"                 # {"migrations":{"version":"V001",...}}
```

Swagger queda en `$API_URL/swagger-ui.html`. Para ver los logs de la aplicación:
`aws logs tail /ecs/franchises-api --follow`.

**Desplegar una versión nueva a mano**: repetir los pasos 3 (sin el `-target`, solo el build y el
push con el nuevo `TAG`) y 4. Con el CD configurado, basta con hacer push a `main`.

### Costo y cómo destruir todo

Con una tarea encendida todo el mes en `us-east-1`: balanceador ~16 USD, Fargate (0,25 vCPU y
0,5 GB) ~9 USD y direcciones IPv4 públicas ~4 USD, unos **30 USD al mes**. ECS Express no tiene
cargo adicional; DynamoDB on-demand, ECR y los logs cuestan centavos con el tráfico de una prueba.

Para no seguir pagando:

```bash
tf destroy -var image_tag=$(tf output -raw image_tag)
```

Borra todo lo creado por Terraform (también las imágenes de ECR). El bucket del estado queda: cuesta
centavos, y se puede vaciar y borrar desde la consola de S3.

### Problemas comunes

| Síntoma | Solución |
|---|---|
| `Unable to locate credentials` o `ExpiredToken` | Configura las credenciales (`aws configure` o `aws sso login`) y exporta `AWS_PROFILE` |
| `EntityAlreadyExists` en `aws_iam_openid_connect_provider.github` | La cuenta ya tiene el proveedor OIDC de GitHub; impórtalo: `tf import aws_iam_openid_connect_provider.github arn:aws:iam::$ACCOUNT:oidc-provider/token.actions.githubusercontent.com` |
| `Error acquiring the state lock` | Otro `apply` (por ejemplo, el CD) está en curso; espera a que termine |
| `No default VPC` o errores de subredes | Crea la VPC por defecto: `aws ec2 create-default-vpc` |
| El servicio no llega a estar sano | Revisa los logs (`aws logs tail /ecs/franchises-api`); suele ser una imagen de otra arquitectura (falta `--platform linux/amd64`) |

## Integración y despliegue continuos

**CI** — cada push y pull request a `main` ejecuta [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

- Levanta `dynamodb-test` y corre `./mvnw verify` (pruebas y umbral de cobertura); publica los
  reportes como artefacto y envía la cobertura a Coveralls.
- En paralelo construye la imagen Docker (con caché de capas) y comprueba que no corra como root.
- Revisa los scripts de desarrollo con ShellCheck.

**CD** — [`.github/workflows/cd.yml`](.github/workflows/cd.yml):

| Evento | Qué hace | Rol de AWS |
|---|---|---|
| Pull request que toca `infra/` | `terraform fmt`, `validate` y `plan` (el plan queda en el resumen del job) | `plan`: solo lectura |
| Push a `main`, cuando el CI termina en verde | Construye la imagen, la sube a ECR con el SHA del commit como tag, aplica Terraform con esa versión y comprueba que la API responda con los datos en la última versión de migración | `deploy`: solo desde el environment `production` |

- **Sin llaves guardadas**: GitHub Actions se autentica en AWS con OIDC. AWS solo entrega
  credenciales temporales si el token viene de este repositorio; para desplegar, además, del
  environment `production` de GitHub, que solo acepta la rama `main`.
- **Mínimo privilegio**: el rol de despliegue solo gestiona los recursos de este proyecto y no puede
  leer datos de la tabla ni modificar los roles del propio pipeline (eso requiere un apply local).
- **Despliegue seguro**: ECS Express despliega la versión nueva de forma gradual y vuelve sola a la
  anterior si falla.

### Activar el CD en tu propio repositorio

Después del primer despliegue local:

1. **Identidad del repositorio**: los roles solo aceptan tokens de un repositorio concreto. GitHub
   los firma con un prefijo que se obtiene así (si devuelve `null` o nada, el prefijo es
   `repo:<propietario>/<repositorio>`):

   ```bash
   gh api repos/<propietario>/<repositorio>/actions/oidc/customization/sub --jq .sub_claim_prefix
   ```

   Si no es el de este repositorio, guárdalo en `infra/terraform.tfvars` (no se versiona) y aplica:

   ```bash
   echo 'github_oidc_subject_prefix = "<prefijo>"' > infra/terraform.tfvars
   tf apply -var image_tag=$TAG
   ```

2. **Variables de GitHub Actions** (son identificadores, no secretos):

   ```bash
   gh variable set AWS_PLAN_ROLE_ARN   --body "$(tf output -raw github_plan_role_arn)"
   gh variable set AWS_DEPLOY_ROLE_ARN --body "$(tf output -raw github_deploy_role_arn)"
   gh variable set TF_STATE_BUCKET     --body "$(tf output -raw state_bucket)"
   ```

3. **Environment `production`**: en GitHub, Settings → Environments → New environment
   `production`, y en "Deployment branches and tags" limitarlo a la rama `main`.

Desde ahí, cada push a `main` despliega solo.

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
- El stock se modifica con un valor absoluto (`{"stock": 40}`), no con sumas o restas.
- El nombre de un producto es único dentro de su sucursal, sin distinguir mayúsculas, tildes ni
  espacios sobrantes: "Café", "cafe" y "  CAFÉ " se consideran el mismo producto (409).
- Producto con más stock por sucursal: se lee con una sola consulta fuertemente consistente. Si hay
  empate se devuelven todos los productos empatados; las sucursales sin productos se omiten; el
  orden es por sucursal y luego por producto.

### Formato de errores

Todos los errores siguen el estándar [ProblemDetail (RFC 7807)](https://www.rfc-editor.org/rfc/rfc7807)
con `Content-Type: application/problem+json`:

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos, JSON mal formado o parámetros con formato incorrecto |
| 404 | La franquicia, sucursal, producto o ruta no existe |
| 409 | El recurso ya existe, o cambió mientras se procesaba la operación (se puede reintentar) |
| 500 | Error inesperado (sin exponer detalles internos) |

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

## Arquitectura

**Stack**: Java 21, Spring Boot 4.1 (WebFlux, Validation, Actuator), AWS SDK v2 asíncrono para
DynamoDB, springdoc-openapi, Maven, JaCoCo y ArchUnit.

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
│   └── dynamodb         Cliente, creación de tabla, migraciones y piezas comunes de persistencia
└── modules              Negocio
    └── franchises       Franquicias, sucursales y productos
        ├── domain
        │   ├── model    Entidades, gateways (interfaces de repositorio) y catálogo de errores
        │   └── usecase  Un caso de uso por acción
        └── infrastructure
            ├── persistence  Implementación de los gateways sobre DynamoDB y migraciones de datos
            └── web          Un controlador por acción, con sus DTOs
```

Reglas verificadas en cada build con ArchUnit (`ArchitectureTest`):

- El dominio de un módulo solo conoce Java, Reactor y el dominio de `core`: nada de Spring, AWS ni HTTP.
- `core` e `integrations` no conocen ningún módulo de negocio.
- La capa web de un módulo no usa su persistencia directamente: siempre pasa por los casos de uso.
- Los módulos no dependen entre sí y nadie depende de `app`.

### Imagen Docker

[`Dockerfile`](Dockerfile) de dos etapas: compila con `maven:3.9.16-eclipse-temurin-21` (las
dependencias en una capa propia, con caché) y ejecuta con `eclipse-temurin:21.0.13_9-jre-noble`
(solo JRE), separando la aplicación en capas de Spring Boot para que un cambio de código solo
reemplace la última. Corre con un usuario sin privilegios (`app`) y su `HEALTHCHECK` usa
`/actuator/health/readiness`. Las pruebas no se ejecutan dentro del build de la imagen: las corre el CI.

## Modelo de datos

Una sola tabla de DynamoDB (`franchises`, on-demand) con clave compuesta `PK` + `SK`. Toda la
información de una franquicia comparte la misma `PK`, así que una sola consulta trae sus
sucursales y productos:

| Entidad | PK | SK | Atributos |
|---|---|---|---|
| Franquicia | `F#{franchiseId}` | `META` | `name` |
| Sucursal | `F#{franchiseId}` | `B#{branchId}` | `name` |
| Producto | `F#{franchiseId}` | `B#{branchId}#P#{productId}` | `name`, `stock` |
| Reserva de nombre | `F#{franchiseId}` | `B#{branchId}#NAME#{nombre normalizado}` | `productId` |

- **Productos como ítems independientes**, no una lista dentro de la sucursal: cada producto se
  modifica sin reescribir la sucursal y no hay riesgo de llegar al límite de 400 KB por ítem.
- **Unicidad del nombre de producto**: DynamoDB no tiene restricciones de unicidad, así que cada
  producto tiene una reserva de nombre que se crea en la misma transacción con
  `attribute_not_exists`. Si dos peticiones intentan el mismo nombre a la vez, solo una gana y la
  otra recibe 409.
- **Eliminar una sucursal sin dejar huérfanos**, aunque lleguen peticiones mientras tanto: primero
  se marca con `status = DELETING` (desde ese instante, agregar o renombrar productos responde 404),
  luego se borran sus productos y reservas, y al final la sucursal. Si el proceso se interrumpe,
  repetir el `DELETE` lo completa. La marca vive en DynamoDB, así que funciona con varias instancias.
- **Escrituras concurrentes**: las operaciones que leen y luego escriben condicionan la escritura a
  lo leído y reintentan; si otro cambio gana repetidamente, responden 409.

En los perfiles `dev` y `test` la aplicación crea la tabla al arrancar si no existe; en AWS la
crea Terraform.

### Migraciones de datos

La **estructura** de la tabla la define Terraform. Los cambios en el **formato de los datos**
(por ejemplo, un atributo nuevo que hay que completar en los productos existentes) son migraciones
versionadas al estilo de Flyway, que la aplicación aplica al arrancar:

- Cada migración es una clase `V<3 dígitos>__<Descripcion>` que implementa `Migration`, en
  `modules/franchises/infrastructure/persistence/migration/`. La primera, `V001__Baseline`, es la
  línea base: representa el modelo de arriba y no transforma nada.
- El historial vive en la propia tabla (`PK = SCHEMA#MIGRATIONS`). Solo se aplican las versiones
  pendientes, en orden; cada una se registra apenas termina.
- Un **lock distribuido** (`PutItem` condicional con vencimiento) garantiza que, si arrancan varias
  instancias a la vez, una sola migre. Si la instancia que migra muere, el lock vence y otra retoma.
- La instancia no se marca lista (`/actuator/health/readiness`) hasta terminar, así que no recibe
  tráfico con los datos a medias. `/actuator/info` publica la versión verificada y el CD comprueba
  que coincida con la última migración del repositorio.

Para agregar una: crear `V002__Descripcion` en ese paquete como `@Component`. Debe ser
**idempotente** (se puede repetir si se interrumpe) y seguir **expand/contract**: durante un
despliegue conviven la versión vieja y la nueva de la app, así que primero se agrega lo nuevo y solo
en una migración posterior se quita lo viejo. Una migración ya desplegada nunca se edita.

## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| [`src/main/java`](src/main/java) | Código de la aplicación (ver [Arquitectura](#arquitectura)) |
| [`src/test/java`](src/test/java) | Pruebas unitarias, de integración, de concurrencia y de arquitectura |
| [`src/main/resources`](src/main/resources) | Configuración por perfil: `dev`, `test` y `prod` (AWS) |
| [`scripts/`](scripts) | `dev.sh`, `test.sh`, `stop.sh` y sus funciones comunes |
| [`infra/`](infra) | Terraform: `main.tf` (DynamoDB, ECR, ECS), `iam.tf`, `cicd.tf` (OIDC), variables y salidas |
| [`.github/workflows/`](.github/workflows) | CI y CD |
| [`Dockerfile`](Dockerfile), [`docker-compose.yml`](docker-compose.yml) | Imagen de la API y entorno local |
