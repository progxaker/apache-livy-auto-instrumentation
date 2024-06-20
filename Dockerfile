FROM registry.access.redhat.com/ubi8/openjdk-17:1.18-2.1705573234 AS agents

WORKDIR /tmp/

RUN curl -fsSLo /tmp/applicationinsights-agent-3.5.3.jar https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.5.3/applicationinsights-agent-3.5.3.jar
RUN curl -fsSLo /tmp/opentelemetry-javaagent-2.5.0.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.5.0/opentelemetry-javaagent.jar

FROM registry.access.redhat.com/ubi8/openjdk-17:1.18-2.1705573234

USER 0

WORKDIR /tmp/

RUN microdnf -y install gzip procps
RUN curl -fsSLo /tmp/spark.tgz https://archive.apache.org/dist/spark/spark-3.5.1/spark-3.5.1-bin-hadoop3.tgz
RUN tar -C /tmp/ -xzf spark.tgz
RUN mv /tmp/spark-3.5.1-bin-hadoop3 /tmp/spark/

WORKDIR /tmp/project/

COPY SimpleApp.java ./src/main/java/SimpleApp.java
COPY pom.xml ./pom.xml

RUN mvn package

COPY --from=agents /tmp/opentelemetry-javaagent-2.5.0.jar /tmp/opentelemetry-javaagent.jar
COPY --from=agents /tmp/applicationinsights-agent-3.5.3.jar /tmp/applicationinsights-agent.jar
COPY applicationinsights.json /tmp/applicationinsights.json

ENV CLASSPATH="/tmp/spark/jars/"

CMD ["/tmp/spark/bin/spark-submit", "--class", "SimpleApp", "--master", "local[4]", "--conf", "spark.jars.ivy=/tmp/.ivy", "target/simple-project-1.0.jar"]
