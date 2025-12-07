package com.example.clinicalextraction.service;

import com.example.clinicalextraction.dto.Entity;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class ClinicalXMLHandler extends DefaultHandler {

    private String currentTag = null;
    private final StringBuilder textBuffer = new StringBuilder();
    private int globalOffset = 0;


    public List<Entity> entities = new ArrayList<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        currentTag = qName;
        textBuffer.setLength(0);
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        String text = new String(ch, start, length);

        textBuffer.append(text);
        globalOffset += text.length();
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (currentTag != null && !textBuffer.isEmpty()) {

            String text = textBuffer.toString();

            int endOffset = globalOffset;
            int startOffset = endOffset - text.length();

            Entity e = Entity.builder()
                    .tag(qName)
                    .text(text.trim())
                    .start(startOffset)
                    .end(endOffset)
                    .build();

            if (!e.getText().isEmpty()) {
                entities.add(e);
            }
        }

        currentTag = null;
    }
}
