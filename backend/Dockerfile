FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /tmp/uploads && chmod 777 /tmp/uploads

EXPOSE 8080

ENTRYPOINT ["java", \
            "-Xmx300m", \
            "-Duser.timezone=GMT+08", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
