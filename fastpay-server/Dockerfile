FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=build /build/target/fastpay-server-1.0.0.jar /app/app.jar

EXPOSE 7001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
