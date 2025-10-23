FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
VOLUME /tmp
ARG JAR_FILE=stats/stats-server/target/stats-server.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]