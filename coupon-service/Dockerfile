# Stage 1: Build
FROM eclipse-temurin:22-jdk-alpine AS builder
WORKDIR /coupon-service-demo
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .
RUN ./mvnw dependency:resolve -q

COPY src/ src/
RUN ./mvnw package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:22-jre-alpine
WORKDIR /coupon-service-demo
COPY --from=builder /coupon-service-demo/target/*.jar coupon-service-demo.jar
ENTRYPOINT ["java", "-jar", "coupon-service-demo.jar"]