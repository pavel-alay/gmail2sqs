package com.alay.util;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextExtractorTest {

    @Test
    void extractText() throws IOException, SAXException {
        String email = "src/test/resources/email01.html";
        String extracted = HtmlTextExtractor.extractText(Files.readAllBytes(Path.of(email)));
        System.out.println(extracted);
        assertThat(extracted).contains("Платеж успешно выполнен Назначение платежа");
    }
}
