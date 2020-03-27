FROM  adoptopenjdk/openjdk12:slim
# --platform=linux/arm/v7
WORKDIR /app

COPY /target/dependency-jars /dependency-jars
COPY target/plc4x-integration-0.1.0-SNAPSHOT-shaded.jar .

ENV EXTERNEL_CORE_IP 127.0.0.1
ENV EXTERNEL_CORE_PORT 1251

CMD ["java", "-jar", "plc4x-integration-0.1.0-SNAPSHOT-shaded.jar"]
