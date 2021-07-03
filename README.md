# Gmail2Sqs

## Build docker image

```bash
mvn clean package -DskipTests && docker build -t gmail2sqs:latest .
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
      SPRING_APPLICATION_JSON: '{
            "gmail": {
            "accessToken": "some-access-token",
            "clientId": "123456789-secret.apps.googleusercontent.com",
            "clientSecret": "AbC-Secret123",
            "encodedUrl": "https://oauth2.googleapis.com/token",
            "ignoreLabels": {
                "add": [],
                "remove": [
                    "unread"
                ]
            },
            "maxSize": 512,
            "readLabels": {
                "add": [
                    "inbox"
                ],
                "remove": [
                    "unread"
                ]
            },
            "refreshToken": "some-refresh-token",
            "searchPatterns": [
                "^Notification.*",
                "уведомление.*"
            ],
            "searchQuery": "label:inbox"
        },
        "sqs": {
            "group-id": "gmail2sqs",
            "timeout": 1000,
            "url": "https://sqs.us-east-1.amazonaws.com/123456789/some-queue"
        }
    }'      
```
