package com.alay.util;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.core.QueueMessageChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConstructorBinding
public class SqsService {

    private final SqsProperties properties;
    private final AmazonSQSAsync amazonSqs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SqsService(SqsProperties properties, AmazonSQSAsync messagingTemplate) {
        this.properties = properties;
        this.amazonSqs = messagingTemplate;
    }

    public void send(final SqsMessage messagePayload) throws JsonProcessingException {
        MessageChannel messageChannel = new QueueMessageChannel(amazonSqs, properties.getUrl());

        Message<String> msg = MessageBuilder.withPayload(objectMapper.writeValueAsString(messagePayload))
            .setHeader("message-group-id", properties.getGroupId())
            .build();

        if (!messageChannel.send(msg, properties.getTimeout())) {
            throw new RuntimeException("Failed to send message");
        }
        log.info("message sent");
    }

}
