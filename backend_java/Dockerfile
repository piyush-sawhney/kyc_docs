FROM gradle:8.12-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r kyc && useradd -r -g kyc kyc
COPY --from=build /app/build/libs/kyc-docs.jar app.jar
USER kyc
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
