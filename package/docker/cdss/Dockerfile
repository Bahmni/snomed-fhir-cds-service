FROM eclipse-temurin:17.0.6_10-jre

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} snomed-fhir-cds-service.jar

ENTRYPOINT exec java $JAVA_OPTS -jar /snomed-fhir-cds-service.jar