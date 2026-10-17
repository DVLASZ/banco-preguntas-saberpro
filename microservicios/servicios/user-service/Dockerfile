# Se construye desde la carpeta microservicios/ (el servicio depende del módulo común bp-platform):
#   docker build -f servicios/user-service/Dockerfile -t bancopreguntas/user-service .

# Etapa 1: compilar el módulo común y el servicio. La caché de Maven se conserva entre construcciones.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -pl servicios/user-service -am package

# Etapa 2: imagen final, solo con el JRE y el jar, y sin ejecutar como root.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/servicios/user-service/target/user-service-*-SNAPSHOT.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
