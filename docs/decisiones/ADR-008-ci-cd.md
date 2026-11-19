# ADR-008: CI/CD con GitHub Actions y acceso a AWS por OIDC

**Estado**: aceptada

## Contexto

Cada cambio debe probarse automáticamente y, si está en `main` y pasa, desplegarse solo, sin
guardar llaves de AWS en GitHub.

## Decisión

**CI** (cada push y pull request): pruebas con cobertura, construcción de la imagen y ShellCheck de
los scripts, en jobs paralelos.

**CD**:

- **Pull request que toca `infra/`**: `fmt`, `validate` y `plan` con un rol de **solo lectura**.
- **Push a `main`**: se dispara cuando el CI termina en verde, así que nunca se despliega un commit
  que no pasó las pruebas. Construye la imagen con el SHA como tag, aplica Terraform con esa versión
  y comprueba que la API responda con los datos en la última versión de migración
  ([ADR-009](ADR-009-migraciones-de-datos.md)). Un despliegue a la vez, nunca cancelado a la mitad.

**Acceso a AWS sin llaves (OIDC)**: GitHub firma un token por ejecución y AWS lo canjea por
credenciales temporales solo si viene de este repositorio. Dos roles:

| Rol | Quién lo puede usar | Permisos |
|---|---|---|
| `plan` | Pull requests y `main` | Solo lectura de los recursos del proyecto y del estado |
| `deploy` | Solo el environment `production`, limitado a la rama `main` | Gestiona los recursos del proyecto; no lee datos de la tabla ni puede modificar los roles del pipeline |

Dos detalles de la configuración OIDC: cuando un job usa un
environment, el token identifica al environment y no a la rama; y GitHub firma los tokens de este
repositorio con un identificador **inmutable** (con los IDs numéricos del propietario y del
repositorio), para que un repositorio borrado y recreado con el mismo nombre no herede el acceso.

**Caché de capas de Docker** en GitHub Actions: Maven Central limita las descargas desde los runners
compartidos (respuestas 429), y la caché evita repetirlas.

## Consecuencias

- Ningún secreto de AWS en el repositorio ni en GitHub; las variables son identificadores.
- Cambiar los permisos del pipeline requiere un `apply` local: un workflow no puede ampliar sus
  propios permisos.
- Compromiso: el rol de despliegue tiene `ecs:*` sin restringir el recurso, porque ECS Express crea
  recursos internos cuyos nombres no se conocen de antemano; el resto de sus permisos sí está
  limitado al proyecto.
