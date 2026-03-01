package com.example.clinicalextraction.service.step1;

import com.example.clinicalextraction.dto.Entity;
import com.example.clinicalextraction.dto.Group;
import com.example.clinicalextraction.dto.Relation;
import com.example.clinicalextraction.entity.PatternRule;
import com.example.clinicalextraction.service.XmlParser;
import com.example.clinicalextraction.util.Patterns;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class Step1ExtractionService {

    private final XmlParser xmlParser;

    public Step1ExtractionService(XmlParser xmlParser) {
        this.xmlParser = xmlParser;
    }

    public List<Group> getData(String xmlPath) throws IOException, SAXException {

        String xml = Files.readString(Paths.get(xmlPath));

        List<String> chunks = xmlParser.chunkXml(xml);

        List<Group> groups = new ArrayList<>();

        for (String chunk : chunks) {
            groups.add(xmlParser.parseChunk1(chunk));
        }

        for (Group group : groups) {
            List<Relation> relation = extractRelations(
                    group.getReference(),
                    group.getEntities(),
                    Patterns.relationTypePatterns()
            );
            group.setSuggestedRelation(relation);
        }

        return groups;
    }


    public List<Relation> extractRelations(
            String text,
            List<Entity> entities,
            Map<String, PatternRule> ruleMap
    ) {

        List<Relation> relations = new ArrayList<>();

        entities.sort(Comparator.comparingInt(Entity::getStart));



            for (PatternRule rule : ruleMap.values()) {

                for (int i = 0; i < entities.size(); i++) {

                    Entity source = entities.get(i);

                    if (!source.getTag().equals(rule.getSourceTag())) {
                        continue;
                    }

                    switch (rule.getGroupingStrategy()) {

                        case SINGLE:
                        case NEAREST:
//                            processNearest(text, entities, rule, source, i, relations);
                            break;

                        case SCOPE_UNTIL_NEXT:
                            processScope(text, entities, rule, source, i, relations);
                            break;
                    }
                }

        }

        return relations;
    }

    private void processScope(
            String text,
            List<Entity> entities,
            PatternRule rule,
            Entity source,
            int sourceIndex,
            List<Relation> relations
    ) {

        Relation relation = Relation.builder()
                .type(rule.getRelationType())
                .source(source.getText())
                .targets(new ArrayList<>())
                .build();

        for (int j = sourceIndex + 1; j < entities.size(); j++) {

            Entity next = entities.get(j);

            if (next.getTag().equals(rule.getSourceTag())) {
                break;
            }

            if (!next.getTag().equals(rule.getTargetTag())) {
                continue;
            }

            String between = text.substring(source.getEnd(), next.getStart());

            if (!rule.getTriggerPattern().matcher(between).find()) {
                continue;
            }

            relation.getTargets().add(next.getText());
        }

        if (!relation.getTargets().isEmpty()) {
            relations.add(relation);
        }
    }
}
