# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl \
	&& mkdir -p /app/uploads

COPY --from=build /app/target/crm-system-*.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
	CMD curl -sf http://localhost:8080/ >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
