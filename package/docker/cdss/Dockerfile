FROM eclipse-temurin:17.0.6_10-jre

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} snomed-fhir-cds-service.jar
COPY CDS_Medication-Condition_Cards.xlsx CDS_Medication-Condition_Cards.xlsx
COPY CDS_Medication-Medication_Cards.tsv CDS_Medication-Medication_Cards.tsv

ENTRYPOINT exec java $JAVA_OPTS -jar /snomed-fhir-cds-service.jar