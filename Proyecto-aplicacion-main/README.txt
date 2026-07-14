## Requisitos
- Java 21

## Configuración necesaria
Variables de entorno 
PORT=27017
DB_HOST=localhost
DB_MONGODB=galeria
Sin autenticación

## Acceso al sistema
URL por defecto (http://localhost:8080)
Sin autenticación

# Galería API / Sistema

Breve descripción de lo que hace tu proyecto (por ejemplo: "API REST desarrollada en Java 21 para la gestión y visualización de una galería de imágenes").

---

## Estructura del Repositorio

Este proyecto sigue una estrategia de ramificación organizada:
* `main` / `master`: Rama principal con el código estable y listo para producción.
* `feature/...` o `dev`: Ramas de trabajo utilizadas para el desarrollo de nuevas funcionalidades.

---

## Requisitos Previos

Antes de empezar, asegúrate de tener instalado:
* **Java 21** (JDK 21)
* **MongoDB** running locally (puerto por defecto `27017`)

---

## Configuración del Entorno

El sistema requiere las siguientes variables de entorno para comunicarse con la base de datos. 

Archivo .env =

PORT=27017
DB_HOST=localhost
DB_MONGODB=galeria
