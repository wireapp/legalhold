This is Legal Hold Service for Wire.

# Environment variables
- SERVICE_TOKEN: 
- WIRE_API_HOST: default: https://prod-nginz-https.wire.com
- DB_DRIVER: default: org.postgresql.Driver
- DB_URL: default: jdbc:postgresql://localhost/legalhold
- DB_USER:
- DB_PASSWORD:

# Endpoints visible to Wire Server
POST    /initiate
POST    /confirm
POST    /remove

# Endpoints visible to Audit
GET     /index.html 
GET     /conv/{conversationId} 
GET     /devices.html 
GET     /events/{conversationId} 
