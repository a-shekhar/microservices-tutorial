FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /home/app
COPY . /home/app
RUN mvn -f /home/app/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jdk 
# Expose port 5000 so Docker knows which port is open
EXPOSE 5000
# Copy the jar file from target folder into the container
COPY --from=build /home/app/target/*.jar app.jar

# Run the Spring Boot app with server.port=5000
ENTRYPOINT ["sh", "-c", "java -jar /app.jar --server.port=5000"]
