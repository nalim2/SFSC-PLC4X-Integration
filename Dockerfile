FROM  adoptopenjdk/openjdk12
# --platform=linux/arm/v7
WORKDIR /app

COPY /target/dependency-jars /dependency-jars
COPY target/plc4x-integration-0.1.0-SNAPSHOT.jar .
COPY target/classes/logback.xml .
ENV EXTERNEL_CORE_IP 127.0.0.1
ENV EXTERNEL_CORE_PORT 1251
ENV RUN_MONITORING_SERVICE true
ENV RUN_READ_SERVICE true
ENV RUN_WRITE_SERVICE false

CMD ["java", "-jar", "plc4x-integration-0.1.0-SNAPSHOT.jar"]
