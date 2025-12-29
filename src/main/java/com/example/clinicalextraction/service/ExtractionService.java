package com.example.clinicalextraction.service;

import com.example.clinicalextraction.Patterns;
import com.example.clinicalextraction.dto.Entity;
import com.example.clinicalextraction.dto.Group;
import com.example.clinicalextraction.dto.Relation;
import com.example.clinicalextraction.entity.PatternRule;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExtractionService {

    private final XmlParser xmlParser;

    private RelationExtractionService relationExtractionService;

    public  ExtractionService(XmlParser xmlParser, RelationExtractionService relationExtractionService) {
        this.xmlParser = xmlParser;
        this.relationExtractionService = relationExtractionService;
    }

    public List<Group> extractRelations(String xmlFilePath) throws IOException, SAXException {
        String xml = Files.readString(Paths.get(xmlFilePath));

        List<String> chunks = xmlParser.chunkXml(xml);

        List<Group> groups = new ArrayList<>();

        for (String chunk : chunks) {
            groups.add(xmlParser.parseChunk(chunk));
        }

        for (Group group : groups) {
            List<Relation> relation = relationExtractionService.extractRelations(
                    group.getReference(),
                    group.getEntities(),
                    Patterns.relationTypePatterns()
            );
            group.setSuggestedRelation(relation);
        }

        return groups;
    }


    public List<Relation> inferRelation(String reference, List<Entity> entities) {
        List<Relation> relations = new ArrayList<>();

        for (List<PatternRule> patternRules : Patterns.relationTypePatterns().values()) {
            for (PatternRule rule : patternRules) {
                Matcher matcher = rule.getTriggerPattern().matcher(reference);

                while (matcher.find()) {
                    int triggerPos = matcher.start();

                    Entity source = entities.stream()
                            .filter(e -> e.getTag().equals(rule.getSourceTag()))
                            .filter(e -> {
                                int entityPos = reference.indexOf(e.getText());
                                if (entityPos >= triggerPos) return false;

                                String between = reference.substring(entityPos + e.getText().length(), triggerPos);
                                return !between.matches(".*\\b(and|or)\\b.*");
                            })
                            .reduce((first, second) -> second)
                            .orElse(null);

                    if (source == null) continue;

                    int nextConjunction = findNextPattern(reference, triggerPos, "\\b(and|or)\\b");
                    int nextTrigger = findNextTrigger(reference, matcher.end(), rule.getTriggerPattern());

                    int boundary = Math.min(
                            nextConjunction != -1 ? nextConjunction : reference.length(),
                            nextTrigger != -1 ? nextTrigger : reference.length()
                    );

                    List<String> targets = entities.stream()
                            .filter(e -> e.getTag().equals(rule.getTargetTag()))
                            .filter(e -> {
                                int entityPos = reference.indexOf(e.getText());
                                return entityPos > triggerPos && entityPos < boundary;
                            })
                            .map(Entity::getText)
                            .collect(Collectors.toList());

                    if (targets.isEmpty()) continue;

                    relations.add(Relation.builder()
                            .type(rule.getRelationType())
                            .source(source.getText())
                            .targets(targets)
                            .build());
                }
            }
        }

        return relations;
    }

    private int findNextPattern(String text, int fromIndex, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(text);
        return m.find(fromIndex) ? m.start() : -1;
    }

    private int findNextTrigger(String text, int fromIndex, Pattern triggerPattern) {
        Matcher m = triggerPattern.matcher(text);
        return m.find(fromIndex) ? m.start() : -1;
    }


}
