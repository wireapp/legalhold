FROM maven:3-openjdk-11 AS build
LABEL description="Wire Legal Hold"
LABEL project="wire-bots:legal-hold"

WORKDIR /app

# download dependencies
COPY pom.xml ./
RUN mvn verify --fail-never -U

# build
COPY . ./
RUN mvn -Dmaven.test.skip=true package

# runtime stage
FROM wirebot/runtime:1.3.0

RUN mkdir /opt/hold
RUN mkdir /opt/hold/images
RUN mkdir /opt/hold/avatars

WORKDIR /opt/hold

# Copy assets
COPY src/main/resources/assets/* /opt/hold/assets/

# Copy configuration
COPY hold.yaml /opt/hold/

# Copy built target
COPY --from=build /app/target/hold.jar /opt/hold/

# create version file
ARG release_version=development
ENV RELEASE_FILE_PATH=/opt/hold/release.txt
RUN echo $release_version > $RELEASE_FILE_PATH

EXPOSE  8080 8081
ENTRYPOINT ["java", "-jar", "hold.jar", "server", "/opt/hold/hold.yaml"]
