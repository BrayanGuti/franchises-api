# ADR-006: Calidad, pruebas y entorno local

**Estado**: aceptada

## Contexto

Buena parte del valor del diseño está en su comportamiento bajo concurrencia, que una prueba
secuencial no demuestra. Además, quien evalúa debería poder correr el proyecto y sus pruebas sin
cuenta de AWS y sin instalar más que Docker.

## Decisión

**Pruebas**

- Por capa: unitarias puras para casos de uso y mapeadores, de controlador con el caso de uso
  simulado, e integración contra DynamoDB Local para persistencia y endpoints.
- Surefire (`*Test`) y Failsafe (`*IT`) con un reporte de JaCoCo unificado; el build falla por
  debajo del **80 %** de cobertura (la actual supera el 95 %). ArchUnit verifica la arquitectura.
- **Concurrencia real**: un cliente HTTP reactivo lanza hasta 50 peticiones simultáneas y las pruebas
  verifican invariantes, no un orden (estado final válido, códigos esperados, cero ítems huérfanos).
- **Integridad referencial**: un verificador recorre los ítems crudos (producto ↔ reserva 1:1, sin
  duplicados) tras cargas mezcladas con semilla fija; tiene sus propias pruebas, que corrompen datos
  a propósito. Estabilidad comprobada: 15 corridas seguidas sin fallos.
- Las pruebas no limpian la tabla: cada una usa IDs únicos.

**Entorno local**

- Dos DynamoDB Local en Docker Compose: `dynamodb-dev` (persistente) y `dynamodb-test` (en memoria,
  forzado en las pruebas), más el inspector `dynamodb-admin` apuntando solo a desarrollo. Todos los
  puertos solo en `127.0.0.1`: DynamoDB Local acepta cualquier credencial y el inspector no tiene
  autenticación.
- Scripts `dev.sh`, `test.sh` y `stop.sh`: Docker como único requisito para correr la API
  (`--maven` para desarrollar sin Docker), datos de ejemplo cargados por la API solo cuando la base
  es nueva, y Swagger y el inspector abiertos al terminar. Funcionan en Linux, macOS y Windows
  (Git Bash o WSL), con fin de línea LF forzado y ShellCheck en el CI.

## Consecuencias

- El CI tarda más que con solo pruebas unitarias, a cambio de probar el comportamiento real.
- Las pruebas de concurrencia no dependen de tiempos (sin `sleep`), lo que las mantiene estables.
- Coveralls publica la cobertura sin poder romper el CI si el servicio externo falla.
