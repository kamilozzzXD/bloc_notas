# ============================================================
# ETAPA 1: BUILD
# Imagen completa con Maven + JDK para compilar el proyecto.
# Esta etapa NO llega a producción, solo genera el .jar.
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build

# Directorio de trabajo dentro del contenedor builder
WORKDIR /build

# ── Copiar el pom.xml primero y descargar dependencias ──────
# Aprovecha la caché de capas de Docker: si el pom no cambia,
# no se vuelven a descargar las dependencias en cada rebuild.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# ── Copiar el código fuente y compilar ──────────────────────
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# ETAPA 2: RUNTIME
# Imagen mínima de JRE (sin Maven, sin JDK) para ejecutar.
# Eclipse Temurin 21 JRE sobre Alpine = imagen muy ligera (~85 MB).
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario no-root por seguridad (buena práctica en contenedores)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Directorio de trabajo del contenedor final
WORKDIR /app

# ── Copiar SOLO el .jar desde la etapa de build ─────────────
COPY --from=build /build/target/backend-0.0.1-SNAPSHOT.jar app.jar

# ── Puerto que expone la aplicación Spring Boot ─────────────
EXPOSE 8080

# ── Variables de entorno esperadas en tiempo de ejecución ───
# NO se definen valores aquí. Se inyectan desde Render/Koyeb/Docker CLI.
# Estas líneas son puramente documentativas (ENV sin valor = ARG hint).
ENV DB_HOST="" \
    DB_PORT="3306" \
    DB_NAME="" \
    DB_USER="" \
    DB_PASS="" \
    JWT_SECRET="" \
    JWT_EXPIRATION_MS="86400000" \
    PORT="8080"

# ── Punto de entrada ─────────────────────────────────────────
# -XX:+UseContainerSupport → respeta los límites de memoria del contenedor
# -XX:MaxRAMPercentage=75.0 → usa máximo el 75% de la RAM asignada al contenedor
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
