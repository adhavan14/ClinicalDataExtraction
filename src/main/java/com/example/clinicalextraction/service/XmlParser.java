package com.example.clinicalextraction.service;

import com.example.clinicalextraction.dto.Relation;
import com.example.clinicalextraction.entity.PatternRule;
import com.example.clinicalextraction.util.Patterns;
import com.example.clinicalextraction.dto.Entity;
import com.example.clinicalextraction.dto.Group;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class XmlParser {

    private static long id = 1;

    private final SAXParser saxParser;

    private List<Entity> entities = new ArrayList<>();

    private Group group = Group.builder().build();

    private StringBuilder references = new StringBuilder();

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

    public List<Group> parseChunk(String chunk) throws SAXException, IOException {

        ClinicalXMLHandler clinicalXMLHandler = new ClinicalXMLHandler();

        String wrappedChunk = "<root>" + chunk + "</root>";

        saxParser.parse(new InputSource(new StringReader(wrappedChunk)), clinicalXMLHandler);

        List<String> reference = extractReference(chunk);

        process();

        return buildGroups(reference, clinicalXMLHandler);
    }

    public void parseChunk1(String chunk, List<Group> groups) throws SAXException, IOException {

        ClinicalXMLHandler clinicalXMLHandler = new ClinicalXMLHandler();

        String wrappedChunk = "<root>" + chunk + "</root>";

        saxParser.parse(new InputSource(new StringReader(wrappedChunk)), clinicalXMLHandler);

        String reference = chunk.replaceAll("<[^>]+>", "").trim();

        buildGroups(reference, clinicalXMLHandler, groups);
    }

    private void buildGroups(String reference, ClinicalXMLHandler clinicalXMLHandler, List<Group> groups) {

        entities.addAll(clinicalXMLHandler.entities);
        references.append(reference).append(" ");
        List<Relation> relations = process();

        if (!relations.isEmpty()) {
            group.setEntities(entities);
            group.setGroupId(id);
            group.setReference(references.toString().trim());
            group.setSuggestedRelation(relations);
            id++;
            groups.add(group);
            group = Group.builder().build();
            references.delete(0, references.length());
            entities = new ArrayList<>();
        }

    }

    private List<Relation> process() {

        Map<String, PatternRule> ruleMap = new HashMap<>();
        ruleMap.put("located_in", PatternRule.builder()
                .sourceTag("condition")
                .targetTag("bodypart")
                .build());

        ruleMap.put("treats", PatternRule.builder()
                .sourceTag("drug")
                .targetTag("condition")
                .build());

        ruleMap.put("cause", PatternRule.builder()
                .sourceTag("drug")
                .targetTag("symptom")
                .build());

        List<Relation> relations = new ArrayList<>();
        for (Map.Entry<String, PatternRule> map : ruleMap.entrySet()) {
            relations.addAll(getRelations(map.getKey(), map.getValue()));
        }

        return relations;
    }

    private @NonNull List<Relation> getRelations(
            String relationType,
            PatternRule rule) {

        List<Relation> relations = new ArrayList<>();

        List<String> currentSources = new ArrayList<>();
        List<String> currentTargets = new ArrayList<>();

        for (Entity entity : entities) {

            if (entity.getTag().equals(rule.getSourceTag())) {

                if (!currentSources.isEmpty() && !currentTargets.isEmpty()) {
                    relations.add(Relation.builder()
                            .type(relationType)
                            .source(new ArrayList<>(currentSources))
                            .targets(new ArrayList<>(currentTargets))
                            .build());
                    currentTargets.clear();
                    currentSources.clear();
                }

                currentSources.add(entity.getText());
            }

            else if (entity.getTag().equals(rule.getTargetTag())) {
                currentTargets.add(entity.getText());
            }
        }

        if (!currentSources.isEmpty() && !currentTargets.isEmpty()) {
            relations.add(Relation.builder()
                    .type(relationType)
                    .source(currentSources)
                    .targets(currentTargets)
                    .build());
        }

        return relations;
    }

    private List<Group> buildGroups(List<String> references, ClinicalXMLHandler clinicalXMLHandler) {
        List<Group> groups = new ArrayList<>();
        Group group = Group.builder().build();
        group.setEntities(new ArrayList<>());
        int refIndex = 0;

        for (Entity entity : clinicalXMLHandler.entities) {
//            if (Patterns.getTriggersForGroup().contains(entity.getTag().toLowerCase())) {
                if (!group.getEntities().isEmpty()) {
                    group.setGroupId(id);
                    System.out.println(refIndex + " " + group.getGroupId() + " " + references.size());
                    group.setReference(references.get(refIndex).replaceAll("\\.$", ""));
                    refIndex++;
                    id++;
                    groups.add(group);
                }
                group = Group.builder().build();
                group.setEntities(new ArrayList<>());
//            }
            group.getEntities().add(entity);
        }

        if (!group.getEntities().isEmpty()) {
            group.setGroupId(id);
            group.setReference(references.get(refIndex).replaceAll("\\.$", ""));
            id++;
            groups.add(group);
        }

        return groups;
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
