# Imagen del backend. Multi-etapa: la imagen final no lleva Maven ni el código
# fuente, solo el jar y un JRE.

# --- Etapa de compilación ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Las dependencias se resuelven antes de copiar el código: así un cambio en un .java
# no invalida la capa de descarga, que es la lenta.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Los tests ya corren en CI y en local; la imagen solo empaqueta.
RUN mvn -B -q clean package -DskipTests

# --- Etapa de ejecución ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios: el proceso no necesita root para escuchar en 8080.
RUN addgroup -S cuentas && adduser -S cuentas -G cuentas
USER cuentas

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

# La configuración (DB_URL, DB_USER, DB_PASS, JWT_SECRET, CORS_ALLOWED_ORIGINS)
# llega por variables de entorno: nada de eso se incrusta en la imagen.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
