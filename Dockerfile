# syntax=docker/dockerfile:1.7

# ---- Etapa 1: compilar con Maven ---------------------------------------------------------------
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace

# Primero solo el pom: las dependencias quedan en una capa que se reutiliza mientras el pom no cambie.
# El caché de ~/.m2 (BuildKit) evita volver a descargarlas entre builds.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

# Las pruebas no corren aquí: necesitan dynamodb-test y se ejecutan en el CI con ./mvnw verify.
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package \
	&& java -Djarmode=tools -jar target/franchises-api-*.jar extract --layers --launcher --destination target/extracted

# ---- Etapa 2: imagen de ejecución (solo JRE) ---------------------------------------------------
FROM eclipse-temurin:21.0.13_9-jre-noble

# curl solo para el HEALTHCHECK
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& groupadd --system app \
	&& useradd --system --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

# Capas de Spring Boot, de la que menos cambia a la que más: un cambio de código solo reemplaza la última.
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

# Nunca se ejecuta como root
USER app

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# "Listo" solo cuando la aplicación terminó de arrancar (readiness), no solo cuando el proceso vive.
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
	CMD curl -fs http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
