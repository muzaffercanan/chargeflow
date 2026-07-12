FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY station-service/pom.xml station-service/pom.xml
COPY session-service/pom.xml session-service/pom.xml

RUN chmod +x mvnw && ./mvnw -B -pl station-service -am dependency:go-offline

COPY station-service/src station-service/src

RUN ./mvnw -B -pl station-service -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 spring

WORKDIR /app

COPY --from=build /workspace/station-service/target/station-service-*.jar app.jar

USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
