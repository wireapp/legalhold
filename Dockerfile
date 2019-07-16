FROM dejankovacevic/bots.runtime:2.10.3

RUN mkdir /opt/hold
RUN mkdir /opt/hold/legalhold

COPY target/hold.jar   /opt/hold/hold.jar
COPY hold.yaml         /etc/hold/hold.yaml
COPY src/main/resources/legalhold/assets/*         /opt/hold/legalhold/assets/

RUN mkdir /opt/hold/legalhold/images
RUN mkdir /opt/hold/legalhold/avatars

WORKDIR /opt/hold
     
EXPOSE  8080 8081 8082

CMD ["sh", "-c","/usr/bin/java -Djava.library.path=/opt/wire/lib -jar hold.jar server /etc/hold/hold.yaml"]
