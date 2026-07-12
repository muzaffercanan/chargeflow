FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY station-service/pom.xml station-service/pom.xml
COPY session-service/pom.xml session-service/pom.xml

RUN chmod +x mvnw && ./mvnw -B -pl session-service -am dependency:go-offline

COPY session-service/src session-service/src

RUN ./mvnw -B -pl session-service -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 spring

WORKDIR /app

COPY --from=build /workspace/session-service/target/session-service-*.jar app.jar

USER spring

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
