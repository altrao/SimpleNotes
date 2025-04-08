 Build stage
FROM gradle:8.4-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/SimpleNotes.jar SimpleNotes.jar

LABEL name="SimpleNotes"

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "SimpleNotes.jar"]