# Galería de Blogs — Plataforma con moderación de imágenes por IA

Aplicación web de blogs con galería de imágenes desarrollada en **Java 21 + Spring Boot**.
Cada usuario puede registrarse, crear su blog personal y publicar posts con imágenes.
Antes de publicarse, **toda imagen se modera automáticamente con la API externa
[Sightengine](https://sightengine.com)**, que bloquea contenido de desnudez, violencia
y drogas. Incluye panel de administración para gestión de usuarios y persistencia
en MongoDB.

**Stack:** Java 21 · Spring Boot · Thymeleaf · MongoDB · Docker · JUnit 5 · JaCoCo · SonarQube

---

## Prerrequisitos

Para levantar con Docker (recomendado):
- [Docker](https://docs.docker.com/get-docker/) 24+ con Docker Compose

Para ejecutar en local sin Docker y/o correr las pruebas:
- **JDK 21**
- **MongoDB 7** corriendo en `localhost:27017` (o vía Docker, ver abajo)
- No necesitas instalar Maven: el proyecto incluye Maven Wrapper (`./mvnw`)

Además:
- Una cuenta gratuita de [Sightengine](https://sightengine.com) para obtener
  `API_USER` y `API_SECRET` (necesarios para la moderación de imágenes).

---

## Configuración

Copia el archivo de ejemplo y completa tus credenciales:

```bash
cd blog-project
cp .env.example .env
```

| Variable | Descripción | Ejemplo |
|---|---|---|
| `SIGHTENGINE_API_USER` | Usuario de la API de Sightengine | `123456789` |
| `SIGHTENGINE_API_SECRET` | Secret de la API de Sightengine | `abc123...` |
| `APP_UPLOADS_DIR` | Carpeta donde se guardan las imágenes subidas | `uploads` |

---

## Levantar la aplicación

### Opción A — Docker Compose (un solo comando)

```bash
cd blog-project
docker compose up --build
```

Esto construye la imagen de la app (build multi-stage con Maven) y levanta
**dos contenedores**: la aplicación y MongoDB, con volúmenes persistentes
para la base de datos y las imágenes subidas.

➡ App disponible en: **http://localhost:8082**

Para detener: `docker compose down` (los datos persisten en los volúmenes).

### Opción B — Local con Maven Wrapper

Requiere MongoDB corriendo en `localhost:27017`. Si no lo tienes instalado:

```bash
docker run -d --name mongo -p 27017:27017 mongo:7
```

Luego:

```bash
cd blog-project
./mvnw spring-boot:run
```

➡ App disponible en: **http://localhost:8082**

> En Windows usa `mvnw.cmd` en lugar de `./mvnw`.

---

## Ejecutar las pruebas

> Las pruebas de integración (`*IT`) usan una base MongoDB real (`testdb` en
> `localhost:27017`), así que asegúrate de tener MongoDB corriendo antes
> (ver comando `docker run` de arriba).

```bash
cd blog-project

# Solo pruebas unitarias (50 tests, no requieren MongoDB)
./mvnw test

# Suite completa: unitarias + integración + reporte de cobertura JaCoCo
./mvnw verify
```

El reporte de cobertura queda en:
`blog-project/target/site/jacoco/index.html`

### Análisis de calidad con SonarQube (opcional)

```bash
./mvnw verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<TU_TOKEN>
```

---

## Acceso al sistema

| Rol | Acceso |
|---|---|
| Visitante | `http://localhost:8082/` — ver blogs y registrarse |
| Usuario | Registro en `/register`, login en `/login`, panel en `/user` |
| Administrador | Panel en `/admin` (gestión y eliminación de usuarios) |

---

## Estructura del proyecto

```
blog-project/
├── src/main/java/com/example/blogproject/
│   ├── domain/          # Modelos y puertos (arquitectura hexagonal)
│   ├── application/     # Servicios de negocio
│   ├── infrastructure/  # MongoDB, Sightengine, almacenamiento de archivos
│   └── web/             # Controladores, DTOs y seguridad de sesión
├── src/test/java/.../Unitarias/    # 50 pruebas unitarias
├── src/test/java/.../Integracion/  # 36 pruebas de integración
├── Dockerfile           # Build multi-stage (Maven → JRE Alpine, usuario no-root)
├── docker-compose.yml   # App + MongoDB con volúmenes persistentes
└── .env.example         # Plantilla de variables de entorno
```
