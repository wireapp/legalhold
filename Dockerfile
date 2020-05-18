FROM dejankovacevic/bots.runtime:2.10.3

RUN mkdir /opt/hold
RUN mkdir /opt/hold/images
RUN mkdir /opt/hold/avatars

COPY target/hold.jar   /opt/hold/hold.jar
COPY hold.yaml         /etc/hold/hold.yaml
COPY src/main/resources/assets/*         /opt/hold/assets/

WORKDIR /opt/hold

# create version file
ARG release_version=development
ENV RELEASE_FILE_PATH=/opt/hold/release.txt
RUN echo $release_version > $RELEASE_FILE_PATH

EXPOSE  8080 8081 8082

ENTRYPOINT ["java", "-javaagent:/opt/wire/lib/jmx_prometheus_javaagent.jar=8082:/opt/wire/lib/metrics.yaml", "-jar", "hold.jar", "server", "/etc/hold/hold.yaml"]
