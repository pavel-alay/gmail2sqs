package com.alay.util;

import lombok.Value;

@Value
public class SqsMessage {
    String from;
    String message;
    long datetime;
}
