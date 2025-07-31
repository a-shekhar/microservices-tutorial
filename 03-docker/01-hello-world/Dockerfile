FROM openjdk:21-slim

# Copy the jar file from target folder into the container
COPY target/*.jar app.jar

# Expose port 5000 so Docker knows which port is open
EXPOSE 5000

# Run the Spring Boot app with server.port=5000
CMD ["java", "-jar", "app.jar", "--server.port=5000"]
