This is Legal Hold Service for Wire.

## Environment variables
- SERVICE_TOKEN: <mandatory>. Must be set to some random value (at least 16 alphanumeric chars)
- WIRE_API_HOST: <optional>. Your Wire Backend host. Default: https://prod-nginz-https.wire.com
- DB_DRIVER: <optional>. Default: org.postgresql.Driver
- DB_URL: <optional>. Default: jdbc:postgresql://localhost/legalhold
- DB_USER: <optional>
- DB_PASSWORD: <optional>

## Build the code
docker build -t $DOCKER_USERNAME/legalhold .

## Prebuilt Docker images (public):
https://quay.io/repository/wire/legalhold?tab=tags

## Run docker container (example)
```
docker run \
-e DB_URL='jdbc:postgresql://localhost/legalhold' \
-e DB_USER='admin' \
-e DB_PASSWORD='s3cret' \
-e SERVICE_TOKEN='secr3t' \
-p 80:8080 \
--name secure-hold --rm quay.io/wire/legalhold:1.0.4
``` 

## Endpoints visible to Wire Server
- POST    /initiate
- POST    /confirm
- POST    /remove

## Endpoints visible to Audit
- POST    /authorize
- GET     /index.html 
- GET     /devices.html 
- GET     /conv/{conversationId} 
- GET     /events/{conversationId} 

## Database format (Events table)
  EventId	| Type	| Time	| Payload
  ------- | ----- | ----- | -------------
cba19d4f-1683-4999-8f88-674048919c8f |	conversation.otr-message-add.asset-data	| 2021-07-11 19:54:33.782647|	{"eventId":"a6fb04dd-e281-11eb-8002-22000a0e7660","messageId":"baac4fdc-3ad1-4402-aaa9-b2ae1bf60080","conversationId":"0c7b391e-737e-471c-8f0c-f0a1b4f40308","clientId":"1c07cb700248848d","userId":"cf65f307-5c00-4afc-911b-f6b91bcc0921","time":"2021-07-11T19:53:24.495Z","assetId":"3-1-e1f4bbed-8f84-4cd1-8b3d-5c1d652a5568","assetToken":"","otrKey":"KhgsmyM2paiMIR7HnNjwubWjSKocSCcM0P/qoTYJJfo=","sha256":"zzHz/1iqMkdU8B3eU7b5EB4aXNdJiLFSfzw9Lee8QOc="}
9ab4f996-3c33-4c4b-9872-628dccebacb6 |	conversation.otr-message-add.image-preview	| 2021-07-11 19:54:33.772223 |	{"eventId":"a6fb04dd-e281-11eb-8002-22000a0e7660","messageId":"baac4fdc-3ad1-4402-aaa9-b2ae1bf60080","conversationId":"0c7b391e-737e-471c-8f0c-f0a1b4f40308","clientId":"1c07cb700248848d","userId":"cf65f307-5c00-4afc-911b-f6b91bcc0921","time":"2021-07-11T19:53:24.495Z","mimeType":"image/png","size":2746,"name":"","width":500,"height":500}
92a067eb-70fe-4c4e-89f2-629af98590f9 | conversation.otr-message-add.new-text | 2021-07-11 19:54:33.618655 | {"eventId":"a6643b34-e281-11eb-8002-22000a0e7660","messageId":"1c7f70e8-342d-4aac-8d6e-aacd7babbfb2","conversationId":"0c7b391e-737e-471c-8f0c-f0a1b4f40308","clientId":"1c07cb700248848d","userId":"cf65f307-5c00-4afc-911b-f6b91bcc0921","time":"2021-07-11T19:53:23.508Z","expireAfterMillis":10000,"text":"https://s3-eu-west-1.amazonaws.com/linkpreview.html","mentions":[]}

