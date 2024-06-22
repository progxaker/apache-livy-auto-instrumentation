FROM registry.access.redhat.com/ubi8/openjdk-17:1.18-2.1705573234 AS spark

WORKDIR /tmp/

USER 0

RUN microdnf install -y gzip

USER 1000

RUN curl -fsSLo /tmp/spark.tgz https://archive.apache.org/dist/spark/spark-3.5.1/spark-3.5.1-bin-hadoop3.tgz
RUN tar -C /tmp/ -xzf spark.tgz
RUN mv /tmp/spark-3.5.1-bin-hadoop3 /tmp/spark/

FROM registry.access.redhat.com/ubi8/openjdk-17:1.18-2.1705573234 AS spark-app

WORKDIR /tmp/project/

COPY src/main/java/com/gitlab/progxaker/sparkapplication/SimpleApp.java ./src/main/java/com/gitlab/progxaker/sparkapplication/SimpleApp.java
COPY pom.xml ./pom.xml

RUN mvn package

FROM registry.access.redhat.com/ubi8/openjdk-17:1.18-2.1705573234 AS agents

WORKDIR /tmp/

RUN curl -fsSLo /tmp/applicationinsights-agent-3.5.3.jar https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.5.3/applicationinsights-agent-3.5.3.jar
RUN curl -fsSLo /tmp/opentelemetry-javaagent-2.5.0.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.5.0/opentelemetry-javaagent.jar

FROM registry.access.redhat.com/ubi8/openjdk-17:1.18-2.1705573234 AS livy-builder

USER 0

WORKDIR /tmp/

RUN microdnf install -y git python3.11 krb5-libs krb5-workstation
RUN update-alternatives --set python /usr/bin/python3 && python -m ensurepip --upgrade && python -m pip install --upgrade pip
RUN pip3 install setuptools cloudpickle requests flake8 flaky pytest

USER 1000

RUN git clone https://github.com/apache/incubator-livy.git

WORKDIR /tmp/incubator-livy
RUN git checkout 78b512658e4baf1183f2b352203ada1928d8111a
RUN mvn package -DskipTests -DskipITs -Dmaven.javadoc.skip=true

FROM registry.access.redhat.com/ubi8/openjdk-17:1.18-2.1705573234

USER 0

RUN microdnf install -y procps
RUN groupadd --gid 1000 livy && useradd --uid 1000 --gid livy --shell /bin/bash --home-dir /tmp/incubator-livy livy

USER 1000

COPY --from=spark /tmp/spark/ /tmp/spark/
COPY --from=spark-app /tmp/project/target/simple-project-1.0.jar /tmp/project/simple-project-1.0.jar
COPY --from=livy-builder /tmp/incubator-livy/ /tmp/incubator-livy/
COPY --from=agents /tmp/opentelemetry-javaagent-2.5.0.jar /tmp/opentelemetry-javaagent.jar
COPY --from=agents /tmp/applicationinsights-agent-3.5.3.jar /tmp/applicationinsights-agent.jar
COPY conf/applicationinsights.json /tmp/applicationinsights.json
COPY conf/spark-defaults.conf /tmp/spark/conf/spark-defaults.conf
COPY conf/livy.conf /tmp/incubator-livy/conf/livy.conf

WORKDIR /tmp/incubator-livy/

ENV SPARK_HOME="/tmp/spark"
ENV LIVY_LOG_DIR="/tmp/livy-logs/"
RUN cp conf/log4j.properties.template conf/log4j.properties && mkdir -p /tmp/livy-logs/

CMD ["/tmp/incubator-livy/bin/livy-server"]
