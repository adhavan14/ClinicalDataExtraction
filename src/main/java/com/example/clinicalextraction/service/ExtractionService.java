// java
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExtractionService {

    private final XmlParser xmlParser;

    public ExtractionService(XmlParser xmlParser) {
        this.xmlParser = xmlParser;
    }

    public List<Group> extractRelations(String xmlFilePath) throws IOException, SAXException {
        String xml = Files.readString(Paths.get(xmlFilePath));

        List<String> chunks = xmlParser.chunkXml(xml);

        List<Group> groups = new ArrayList<>();

        for (String chunk : chunks) {
            List<Group> parsed = xmlParser.parseChunk(chunk);
            groups.addAll(parsed);
        }

        Map<String, List<Entity>> context = new HashMap<>();

        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);

            for (Entity e : group.getEntities()) {
                context.computeIfAbsent(e.getTag().toLowerCase(), k -> new ArrayList<>()).add(e);
            }

            Relation relation = inferRelationWithContext(group.getReference(), group.getEntities(), context);
            group.setSuggestedRelation(relation);
        }

        return groups;
    }


    private Relation inferRelationWithContext(String sentenceText, List<Entity> entities, Map<String, List<Entity>> context) {
        List<Entity> current = entities == null ? List.of() : entities;

        for (PatternRule rule : Patterns.relationTypePatterns().values()) {
            boolean triggerMatched = rule.getTriggerPattern().matcher(sentenceText).find();

            Entity source = findFirstByTag(current, rule.getSourceTag());
            if (source == null) source = getLastFromContext(context, rule.getSourceTag());

            List<String> targets = current.stream()
                    .filter(e -> e.getTag().equalsIgnoreCase(rule.getTargetTag()))
                    .map(Entity::getText)
                    .collect(Collectors.toList());
            if (targets.isEmpty()) {
                List<Entity> ctxTargets = context.getOrDefault(rule.getTargetTag().toLowerCase(), List.of());
                targets = ctxTargets.stream().map(Entity::getText).collect(Collectors.toList());
            }

            if (source != null && !targets.isEmpty() && triggerMatched) {
                return Relation.builder()
                        .type(rule.getRelationType())
                        .source(source.getText())
                        .targets(targets)
                        .build();
            }
        }

        return null;
    }

    private Entity findFirstByTag(List<Entity> entities, String tag) {
        return entities.stream()
                .filter(e -> e.getTag().equalsIgnoreCase(tag))
                .findFirst()
                .orElse(null);
    }

    private Entity getLastFromContext(Map<String, List<Entity>> context, String tag) {
        List<Entity> list = context.get(tag.toLowerCase());
        if (list == null || list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }
}
