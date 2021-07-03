package com.alay.util;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

public class HtmlTextExtractor {

    static String extractText(byte[] body) throws IOException, SAXException {
        InputSource is = new InputSource(new ByteArrayInputStream(body));
        is.setEncoding(StandardCharsets.UTF_8.name());
        DOMParser parser = new DOMParser();
        parser.parse(is);
        Document document = parser.getDocument();

        Element root = document.getDocumentElement();
        NodeList styles = document.getElementsByTagName("style");
        IntStream.range(0, styles.getLength())
            .mapToObj(i -> (Element) styles.item(i))
            .forEach(e -> e.getParentNode().removeChild(e));

        return root.getTextContent().replaceAll("[\\s\\h]", " ");
    }
}
