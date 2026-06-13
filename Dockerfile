# =============================================================================
# MS-APPOINTMENT — Dockerfile multi-stage
# Contexto de build: carpeta raíz de ms-appointment/
#   docker build -t ms-appointment:latest .
# =============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /app

# 1. Copia solo los descriptores de build primero → capa cacheada
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle.kts .
COPY build.gradle.kts .

# 2. Descarga dependencias (se cachea mientras build.gradle.kts no cambie)
RUN ./gradlew dependencies --no-daemon --quiet || true

# 3. Copia el código fuente y construye el fat JAR
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# 4. Selecciona únicamente el fat JAR (excluye el plain JAR generado por Gradle)
RUN find /app/build/libs -name "*.jar" ! -name "*plain*" -exec cp {} /app/application.jar \;

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble AS runtime

# Metadatos de la imagen
LABEL maintainer="hospital-team" \
      service="ms-appointment" \
      version="1.0"

WORKDIR /app

# Crea usuario no-root para producción (principio de mínimo privilegio)
RUN groupadd --system hospital \
 && useradd  --system --gid hospital --no-create-home msappointment

# Copia el fat JAR desde el stage de build
COPY --from=builder --chown=msappointment:hospital /app/application.jar app.jar

USER msappointment

# Puerto de la aplicación
EXPOSE 8082

# JVM flags recomendados para contenedores:
#   -XX:+UseContainerSupport  → detecta límites de CPU/Memoria del contenedor
#   -XX:MaxRAMPercentage=75   → usa el 75% de la RAM asignada al pod
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
