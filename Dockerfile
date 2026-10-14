FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN chmod +x mvnw

COPY src src

RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 10001 appuser

COPY --from=build /workspace/target/CodeReviewAPI-0.0.1-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]