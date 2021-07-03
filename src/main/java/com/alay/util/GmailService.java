package com.alay.util;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GmailService {

    private static final MessagePartHeader DEFAULT_FROM = new MessagePartHeader().setValue("unknown");
    private static final ZoneId TARGET_TIME_ZONE = ZoneId.of("Europe/Minsk");
    private static final String ME = "me";
    private static final List<String> UNREAD_LABEL = List.of("UNREAD");
    public static final String GMAIL_FIELDS = "id,internalDate,payload,labelIds,snippet";

    private final Gmail gmail;
    private final int maxSize;
    private final String searchQuery;
    private final List<Predicate<String>> searchPredicates;
    private final List<String> readLabelsAdd;
    private final List<String> readLabelsRemove;

    private final List<String> ignoreLabelsAdd;
    private final List<String> ignoreLabelsRemove;


    public GmailService(GmailProperties gmailProperties) throws GeneralSecurityException, IOException {
        log.info("Search queue: {}", gmailProperties.getSearchQuery());
        log.info("Search patterns: {}", Arrays.toString(gmailProperties.getSearchPatterns()));
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        gmail = new Gmail.Builder(transport, jsonFactory,
            createCredentialWithRefreshToken(transport, jsonFactory, gmailProperties))
            .setApplicationName("Gmail to SQS Forwarder")
            .build();
        searchQuery = gmailProperties.getSearchQuery();
        maxSize = gmailProperties.getMaxSize();
        searchPredicates = Arrays.stream(gmailProperties.getSearchPatterns())
            .map(Pattern::compile)
            .map(Pattern::asMatchPredicate)
            .collect(Collectors.toList());

        Map<String, String> labelIds = gmail.users()
            .labels()
            .list(ME)
            .execute()
            .getLabels()
            .stream()
            .collect(Collectors.toMap(label -> label.getName().toLowerCase(), Label::getId));

        ignoreLabelsAdd = convertToLabelIds(labelIds, gmailProperties.getIgnoreLabels().getAdd());
        ignoreLabelsRemove = convertToLabelIds(labelIds, gmailProperties.getIgnoreLabels().getRemove());
        readLabelsAdd = convertToLabelIds(labelIds, gmailProperties.getReadLabels().getAdd());
        readLabelsRemove = convertToLabelIds(labelIds, gmailProperties.getReadLabels().getRemove());
    }

    public Iterable<Message> getUnreadMessages() throws IOException {
        // messagesResponse.getNextPageToken() is ignored, the rest messages will be fetched during the next run.
        ListMessagesResponse messagesResponse = gmail.users().messages()
            .list(ME)
            .setQ(searchQuery)
            .setLabelIds(UNREAD_LABEL)
            .execute();

        List<Message> result = new ArrayList<>();
        Iterable<Message> messages = batchRead(messagesResponse.getMessages());
        for (Message message : messages) {
            if (filter(message)) {
                result.add(message);
            } else {
                log.info("Ignore message: {}, {}", getSubject(message), getDate(message));
                ignore(message);
            }
        }

        return result;
    }

    public void markAsRead(Message message) throws IOException {
        gmail.users().messages()
            .modify(ME, message.getId(),
                new ModifyMessageRequest()
                    .setAddLabelIds(readLabelsAdd)
                    .setRemoveLabelIds(readLabelsRemove)
            ).execute();
    }

    void ignore(Message message) throws IOException {
        gmail.users().messages()
            .modify(ME, message.getId(),
                new ModifyMessageRequest()
                    .setAddLabelIds(ignoreLabelsAdd)
                    .setRemoveLabelIds(ignoreLabelsRemove)
            ).execute();
    }

    public String getFrom(Message message) {
        return message.getPayload().getHeaders().stream()
            .filter(h -> "From".equals(h.getName()))
            .findFirst()
            .orElse(DEFAULT_FROM)
            .getValue();
    }

    public long getDate(Message message) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(message.getInternalDate() / 1000, 0,
            ZoneOffset.UTC);
        return ZonedDateTime.of(dateTime, ZoneOffset.UTC)
            .withZoneSameInstant(TARGET_TIME_ZONE).toEpochSecond();
    }

    public String getSubject(Message msg) {
        return msg.getPayload().getHeaders().stream()
            .filter(h -> {
                return "Subject".equals(h.getName());
            })
            .findFirst()
            .orElseGet(() -> new MessagePartHeader().setValue("unknown"))
            .getValue();
    }

    @SneakyThrows
    public String getContent(Message msg) {
        MessagePart body = getBodyPart(msg);
        String content;
        if ("text/plain".equals(body.getMimeType())) {
            content = new String(body.getBody().decodeData());
        } else {
            content = HtmlTextExtractor.extractText(body.getBody().decodeData());
        }

        content = content.replaceAll("[\\s\\h]", " ").trim();
        return content.substring(0, Math.min(content.length() - 1, maxSize));
    }

    boolean filter(Message msg) {
        return searchPredicates.stream().anyMatch(p -> p.test(msg.getSnippet()));
    }

    static List<String> convertToLabelIds(Map<String, String> labelIds, String[] labelNames) {
        return Arrays.stream(labelNames)
            .map(labelIds::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    Iterable<Message> batchRead(List<Message> messages) throws IOException {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        final Queue<Message> fetchedMessages = new ConcurrentLinkedQueue<>();

        JsonBatchCallback<Message> callback = new JsonBatchCallback<>() {
            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                log.error(e.getMessage());
            }

            @Override
            public void onSuccess(Message message, HttpHeaders responseHeaders) {
                if (notInSent(message)) {
                    fetchedMessages.add(message);
                }
            }
        };

        BatchRequest batch = gmail.batch();
        for (Message message : messages) {
            gmail.users().messages().get(ME, message.getId())
                .setFields(GMAIL_FIELDS)
                .queue(batch, callback);
        }
        batch.execute();

        return fetchedMessages;
    }

    static boolean notInSent(Message msg) {
        return !msg.getLabelIds().contains("SENT");
    }

    static MessagePart getBodyPart(Message msg) {
        if (msg.getPayload().getParts() != null) {
            for (MessagePart part : msg.getPayload().getParts()) {
                if (part.getBody().getData() != null)
                    return part;
            }
        }

        return msg.getPayload();
    }


    private static Credential createCredentialWithRefreshToken(HttpTransport transport, JsonFactory jsonFactory, GmailProperties properties) {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(properties.getAccessToken());
        tokenResponse.setRefreshToken(properties.getRefreshToken());

        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(transport)
            .setJsonFactory(jsonFactory)
            .setTokenServerUrl(new GenericUrl(properties.getEncodedUrl()))
            .setClientAuthentication(new ClientParametersAuthentication(
                properties.getClientId(),
                properties.getClientSecret()))
            .build()
            .setFromTokenResponse(tokenResponse);
    }
}
