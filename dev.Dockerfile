# Similar to the Dockerfile, but the JAR needs to be built first. Useful for Maven optimizations or for using the local .m2
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /opt/hold

# runtime stage
FROM wirebot/runtime:1.4.0

RUN mkdir /opt/hold
RUN mkdir /opt/hold/images
RUN mkdir /opt/hold/avatars

# Copy assets
COPY src/main/resources/assets/* /opt/hold/assets/

# Copy configuration
COPY hold.yaml /opt/hold/

# Copy the JAR file into the container
COPY target/hold.jar /opt/hold/

# create version file
ARG release_version=development
ENV RELEASE_FILE_PATH=/opt/hold/release.txt
RUN echo $release_version > $RELEASE_FILE_PATH

EXPOSE  8080 8081
ENTRYPOINT ["java", "-jar", "/opt/hold/hold.jar", "server", "/opt/hold/hold.yaml"]