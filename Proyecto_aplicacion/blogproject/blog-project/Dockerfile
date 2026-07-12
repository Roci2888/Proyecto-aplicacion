# Usar la imagen oficial de Eclipse Temurin con JDK 21
FROM eclipse-temurin:21-jdk-alpine
LABEL authors="rocio"
# Establecer el directorio de trabajo
WORKDIR /app
# Copiar el archivo JAR de la aplicación
COPY target/*.jar app.jar
# Exponer el puerto (ajusta según tu aplicación)
EXPOSE 8080
# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]