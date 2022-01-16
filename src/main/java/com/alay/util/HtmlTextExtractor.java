package com.alay.util;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HtmlTextExtractor {

    static String extractText(byte[] body) throws IOException, SAXException {
        InputSource is = new InputSource(new ByteArrayInputStream(body));
        is.setEncoding(StandardCharsets.UTF_8.name());
        DOMParser parser = new DOMParser();
        parser.parse(is);
        StringBuilder textContent = new StringBuilder();
        handleNode(parser.getDocument().getDocumentElement(), textContent);

        return textContent.toString().replaceAll("[\\s\\h]+", " ")
            .replaceAll(" ([.,]) ", "$1 ");
    }

    private static void handleNode(Node node, StringBuilder buffer) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            if (!"style".equalsIgnoreCase(node.getParentNode().getNodeName())) {
                buffer.append(node.getTextContent()).append(" ");
            }
        } else if (node.hasChildNodes()) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                handleNode(childNodes.item(i), buffer);
            }
        }
    }
}
