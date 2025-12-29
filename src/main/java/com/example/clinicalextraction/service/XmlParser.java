package com.example.clinicalextraction.service;

import com.example.clinicalextraction.Patterns;
import com.example.clinicalextraction.dto.Entity;
import com.example.clinicalextraction.dto.Group;
import com.example.clinicalextraction.dto.Relation;
import com.example.clinicalextraction.entity.PatternRule;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class XmlParser {

    private static long id = 1;

    private final SAXParser saxParser;

    public XmlParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParser = saxParserFactory.newSAXParser();
    }

    private List<String> extractReference(String chunk) {
        String[] conditions = chunk.split("\\s+and\\s+(?=<condition>)");
        return Stream.of(conditions)
                .map(condition -> condition.replaceAll("<[^>]+>", "").trim())
                .collect(Collectors.toList());
    }

    public Group parseChunk(String chunk) throws SAXException, IOException {

        ClinicalXMLHandler clinicalXMLHandler = new ClinicalXMLHandler();

        String wrappedChunk = "<root>" + chunk + "</root>";

        saxParser.parse(new InputSource(new StringReader(wrappedChunk)), clinicalXMLHandler);

        String reference = chunk.replaceAll("<[^>]+>", "").trim();

        System.out.println(reference);

//        List<String> reference = extractReference(chunk);
//
        return buildGroups(reference, clinicalXMLHandler);
    }

    private Group buildGroups(String references, ClinicalXMLHandler clinicalXMLHandler) {
        List<Group> groups = new ArrayList<>();
        Group group = Group.builder().build();
        group.setEntities(new ArrayList<>());

        for (Entity entity : clinicalXMLHandler.entities) {
            group.getEntities().add(entity);
        }

        group.setGroupId(id);
        group.setReference(references);
        id++;

        return group;
    }

    public List<String> chunkXml(String xml) {
        List<String> chunks = new ArrayList<>();

        xml = xml.replaceFirst("^\\s*<root>", "")
                .replaceFirst("</root>\\s*$", "");

        String[] sentences = xml.split("(?<=[.!?])\\s+(?=<|[^<])");

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                chunks.add(trimmed);
            }
        }

        return chunks;
    }
}
