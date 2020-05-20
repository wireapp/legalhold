FROM docker.io/maven AS build-env

WORKDIR /app

# download dependencies
COPY pom.xml ./
RUN mvn verify --fail-never -U

# build
COPY . ./
RUN mvn -Dmaven.test.skip=true package

# runtime stage
FROM dejankovacevic/bots.runtime:2.10.3

RUN mkdir /opt/hold
RUN mkdir /opt/hold/images
RUN mkdir /opt/hold/avatars

WORKDIR /opt/hold

# Copy assets
COPY src/main/resources/assets/*         /opt/hold/assets/

# Copy configuration
COPY hold.yaml /opt/hold/

# Copy built target
COPY --from=build-env /app/target/hold.jar /opt/hold/

# create version file
ARG release_version=development
ENV RELEASE_FILE_PATH=/opt/hold/release.txt
RUN echo $release_version > $RELEASE_FILE_PATH

EXPOSE  8080 8081 8082
ENTRYPOINT ["java", "-javaagent:/opt/wire/lib/jmx_prometheus_javaagent.jar=8082:/opt/wire/lib/metrics.yaml", "-jar", "hold.jar", "server", "/opt/hold/hold.yaml"]
