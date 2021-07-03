package com.alay.util;

import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class GmailWatcher {

    GmailService gmail;
    SqsService sqsService;

    public GmailWatcher(GmailService gmail, SqsService sqsService) {
        this.gmail = gmail;
        this.sqsService = sqsService;
    }

    @Scheduled(fixedDelay = 15000)
    public void checkNewMail() throws IOException {
        for (Message email : gmail.getUnreadMessages()) {
            try {
                sqsService.send(message2Event(email));
                gmail.markAsRead(email);
                log.info("message enqueued");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    private SqsMessage message2Event(Message email) {
        return new SqsMessage(gmail.getFrom(email), gmail.getContent(email), gmail.getDate(email));
    }
}
