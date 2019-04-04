FROM dejankovacevic/bots.runtime:2.10.3

COPY target/hold.jar   /opt/hold/hold.jar
COPY hold.yaml         /etc/hold/hold.yaml

RUN mkdir /opt/hold/data
RUN mkdir /opt/hold/data/assets

WORKDIR /opt/hold
     
EXPOSE  8080 8081 8082
