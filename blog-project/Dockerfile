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
# Usuario sin privilegios (buena práctica de seguridad)
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
# Carpeta para las imágenes subidas
RUN mkdir -p /app/uploads && chown -R app:app /app
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]