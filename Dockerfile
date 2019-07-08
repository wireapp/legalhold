FROM dejankovacevic/bots.runtime:2.10.3

COPY target/hold.jar   /opt/legalhold/hold.jar
COPY hold.yaml         /etc/legalhold/hold.yaml

RUN mkdir /opt/legalhold/data
RUN mkdir /opt/legalhold/data/assets
RUN mkdir /opt/legalhold/images
RUN mkdir /opt/legalhold/avatars
             
WORKDIR /opt/legalhold
     
EXPOSE  8080 8081 8082

CMD ["sh", "-c","/usr/bin/java -Djava.library.path=/opt/wire/lib -jar hold.jar server /etc/hold/hold.yaml"]
