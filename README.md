# Gmail2Sqs

## Build docker image

```bash
./mvnw -DskipTests spring-boot:build-image -Dspring-boot.build-image.imageName=gmail2sqs
```

## Docker compose
```
---
version: "2.1"
services:
  gmail2sqs:
    image: gmail2sqs
    container_name: gmail2sqs
    network_mode: bridge
    restart: unless-stopped
    environment:
      AWS_REGION: us-east-1
      AWS_ACCESS_KEY_ID: AKEXAMPLE
      AWS_SECRET_ACCESS_KEY: secretaccesskey123
```
