This is Legal Hold Service for Wire.

## Environment variables
- SERVICE_TOKEN: <mandatory>. Must be set to some random value (at least 16 alphanumeric chars)
- WIRE_API_HOST: <optional>. Your Wire Backend host. Default: https://prod-nginz-https.wire.com
- DB_DRIVER: <optional>. Default: org.postgresql.Driver
- DB_URL: <mandatory>. Default: jdbc:postgresql://localhost/legalhold
- DB_USER:
- DB_PASSWORD:

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
