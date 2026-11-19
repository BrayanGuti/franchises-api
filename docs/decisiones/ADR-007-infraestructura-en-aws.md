# ADR-007: Infraestructura simple con Docker, Terraform y ECS Express Mode

**Estado**: aceptada

## Contexto

La API debe quedar desplegada en la nube con infraestructura como código, accesible por HTTPS, y
poder recrearse o destruirse con un comando. El objetivo explícito fue mantenerla **simple** para el
alcance de esta prueba.

## Opciones evaluadas

| Opción | A favor | En contra |
|---|---|---|
| ECS Fargate con VPC, subredes y balanceador propios, en módulos de Terraform | Control total de la red | Mucho código y muchas piezas para una sola API |
| App Runner | Lo más simple | Cerrado a clientes nuevos desde el 30 de abril de 2026 |
| ECS Fargate sin balanceador | Barato | La IP pública cambia en cada despliegue; sin HTTPS |
| **ECS Express Mode en la VPC por defecto** | AWS administra el balanceador, los grupos de seguridad y el certificado; URL HTTPS estable; despliegues graduales con vuelta atrás | Menos control de la red; requiere la VPC por defecto |

## Decisión

**Imagen Docker** de dos etapas: compila dentro con `maven:3.9.16-eclipse-temurin-21` (dependencias
en una capa con caché) y ejecuta con `eclipse-temurin:21.0.13_9-jre-noble`, separando la aplicación
en capas de Spring Boot. Usuario sin privilegios, `HEALTHCHECK` sobre readiness y `.dockerignore`
como lista blanca. Las pruebas no corren dentro del build: las ejecuta el CI.

**Terraform** en una sola carpeta [`infra/`](../../infra), sin módulos:

- **DynamoDB** on-demand, cifrada y con recuperación a un punto en el tiempo.
- **ECR** con tags inmutables (el SHA del commit), escaneo de vulnerabilidades y solo las últimas 10.
- **ECS Express Mode** con 0,25 vCPU y 512 MB.
- **Tres roles de mínimo privilegio**: el de la tarea solo tiene seis acciones sobre su tabla; el de
  ejecución solo descarga su imagen y escribe sus logs; el de infraestructura usa la política
  administrada de ECS Express. El servicio depende explícitamente de ellos: si un rol se borra antes
  que el servicio, este queda trabado drenando por más de una hora.
- **Estado remoto en S3** con bloqueo nativo (`use_lockfile`), porque el CD también ejecuta
  Terraform; el bucket se crea con dos comandos de la CLI.

## Consecuencias

- Unos 30 USD al mes con una tarea encendida, dominados por el balanceador.
- Pocos recursos que mantener: 12 para la aplicación.
- La imagen ocupa unos 490 MB (unos 150 MB comprimida en ECR); una base distroless o Alpine la
  reduciría.
- Si se necesitara una red propia, se puede volver a una VPC dedicada sin cambiar la aplicación.
