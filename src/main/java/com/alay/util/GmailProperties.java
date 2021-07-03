package com.alay.util;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConstructorBinding
@ConfigurationProperties(prefix = "gmail")
@Value
public class GmailProperties {

    @ConstructorBinding
    @Value
    public static class Labels {
        public Labels(String[] add, @DefaultValue("unread") String[] remove) {
            this.add = add;
            this.remove = remove;
        }

        String[] add;
        String[] remove;
    }

    String accessToken;
    String refreshToken;
    String clientId;
    String clientSecret;
    String encodedUrl;

    String searchQuery;
    String[] searchPatterns;
    int maxSize;

    Labels readLabels;
    Labels ignoreLabels;
}
