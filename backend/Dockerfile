FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/codenest-backend-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

CMD ["sh", "-c", "for k in PORT POSTGRES_HOST POSTGRES_PORT POSTGRES_DATABASE POSTGRES_USER POSTGRES_PASSWORD CLERK_ISSUER CLERK_JWKS_URI; do v=$(printenv \"$k\" || true); if [ -n \"$v\" ]; then echo \"$k=set len=${#v}\"; else echo \"$k=missing\"; fi; done; java -jar /app/app.jar; code=$?; echo \"java exited with status $code\"; exit $code"]
