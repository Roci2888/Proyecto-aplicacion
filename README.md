# ArtGallery BlogApp
![Java 21](https://img.shields.io/badge/Java-21-blue?logo=java&logoColor=white)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=spring&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-%2347A248?logo=mongodb&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-available-blue?logo=docker&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/TailwindCSS-available-teal?logo=tailwindcss&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

Descripción breve
-----------------
ArtGallery BlogApp es una aplicación web tipo red social que permite a los usuarios registrarse, crear su blog personal y publicar entradas con imágenes. Las imágenes se moderan automáticamente para detectar contenido inapropiado y existe un panel administrativo para la gestión de usuarios.

Tabla de contenidos
-------------------
- [Funcionalidades](#funcionalidades)
- [Tecnologías usadas](#tecnologías-usadas)
- [Requisitos previos](#requisitos-previos)
- [Instalación y ejecución](#instalación-y-ejecución)
  - [Con Docker (recomendado)](#con-docker-recomendado)
  - [Sin Docker (desarrollo local)](#sin-docker-desarrollo-local)
- [Variables de entorno](#variables-de-entorno)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Cómo ejecutar los tests](#cómo-ejecutar-los-tests)
- [Autores](#autores)
- [Licencia](#licencia)

Funcionalidades
---------------
- Registro e inicio de sesión de usuarios (contraseñas hasheadas con BCrypt).
- Creación de un blog personal (uno por usuario).
- Publicación de entradas con título, contenido e imagen (por URL).
- Moderación automática de imágenes mediante la API de Sightengine (bloqueo de contenido inapropiado).
- Panel de administración para gestionar usuarios.
- Paginación de entradas.
- Plantillas Thymeleaf para vistas del frontend.

Tecnologías usadas
------------------
- Java 21
- Spring Boot 3.5
- MongoDB
- Thymeleaf
- TailwindCSS
- Sightengine API (moderación de imágenes)
- Maven (con wrapper ./mvnw)
- Docker & Docker Compose

Requisitos previos
------------------
- Git
- Docker y Docker Compose (si vas a usar contenedores)
- JDK 21 (para ejecución local sin Docker)
- MongoDB si ejecutas localmente sin Docker
- Credenciales de Sightengine para moderación de imágenes

Instalación y ejecución
-----------------------

Con Docker (recomendado)
------------------------
1. Clona el repositorio:
   ```bash
   git clone <URL_DEL_REPOSITORIO>
   cd Proyecto-aplicacion
