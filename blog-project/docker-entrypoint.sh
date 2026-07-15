#!/bin/sh
set -e

# Los volúmenes (Railway, Docker) se montan como root, así que el contenedor
# arranca como root solo para dar la propiedad de la carpeta de subidas al
# usuario "app" y después ejecuta la aplicación con ese usuario sin privilegios.
UPLOADS_DIR="${APP_UPLOADS_DIR:-/app/uploads}"

mkdir -p "$UPLOADS_DIR"
chown -R app:app "$UPLOADS_DIR"

exec su-exec app java -jar /app/app.jar