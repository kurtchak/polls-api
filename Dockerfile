FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY polls-domain/build.gradle.kts polls-domain/
COPY polls-sync/build.gradle.kts polls-sync/
COPY polls-bff/build.gradle.kts polls-bff/
COPY polls-app/build.gradle.kts polls-app/
RUN ./gradlew dependencies --no-daemon || true
COPY polls-domain/src polls-domain/src
COPY polls-sync/src polls-sync/src
COPY polls-bff/src polls-bff/src
COPY polls-app/src polls-app/src
RUN ./gradlew :polls-app:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/polls-app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
