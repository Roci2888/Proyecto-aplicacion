# ===== Etapa 1: compilación =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Copiar primero el pom para cachear la descarga de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Ahora sí el código fuente
COPY src ./src
RUN mvn package -DskipTests -B

# ===== Etapa 2: imagen final =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Usuario sin privilegios (buena práctica de seguridad) y su-exec para
# poder bajar de root a ese usuario desde el entrypoint
RUN apk add --no-cache su-exec && addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
# Carpeta para las imágenes subidas
RUN chmod +x /docker-entrypoint.sh && mkdir -p /app/uploads && chown -R app:app /app
EXPOSE 8082
# El entrypoint arranca como root, da permisos sobre APP_UPLOADS_DIR (necesario
# cuando es un volumen montado, p. ej. /data/uploads en Railway) y ejecuta la
# app como el usuario "app"
ENTRYPOINT ["/docker-entrypoint.sh"]
