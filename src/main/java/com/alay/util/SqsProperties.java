package com.alay.util;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConstructorBinding
@ConfigurationProperties(prefix = "sqs")
@Value
public class SqsProperties {

    public SqsProperties(String url, String groupId, @DefaultValue("1000") int timeout) {
        this.url = url;
        this.groupId = groupId;
        this.timeout = timeout;
    }

    String url;
    String groupId;
    int timeout;
}
