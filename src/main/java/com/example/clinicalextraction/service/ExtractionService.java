package com.example.clinicalextraction.service;

import com.example.clinicalextraction.Patterns;
import com.example.clinicalextraction.dto.Entity;
import com.example.clinicalextraction.dto.Group;
import com.example.clinicalextraction.dto.Relation;
import com.example.clinicalextraction.entity.PatternRule;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExtractionService {

    private final XmlParser xmlParser;

    public  ExtractionService(XmlParser xmlParser) {
        this.xmlParser = xmlParser;
    }

    public List<Group> extractRelations(String xmlFilePath) throws IOException, SAXException {
        String xml = Files.readString(Paths.get(xmlFilePath));

        List<String> chunks = xmlParser.chunkXml(xml);

        List<Group> groups = new ArrayList<>();

        for (String chunk : chunks) {
            groups.addAll(xmlParser.parseChunk(chunk));
        }

        for (Group group : groups) {
            Relation relation = inferRelation(group.getReference(), group.getEntities());
            group.setSuggestedRelation(relation);
        }

        return groups;
    }


    public Relation inferRelation(String reference, List<Entity> entities) {

        for (PatternRule rule : Patterns.relationTypePatterns().values()) {

            if (!rule.getTriggerPattern().matcher(reference).find()) continue;

            Entity source = entities.stream()
                    .filter(e -> e.getTag().equals(rule.getSourceTag()))
                    .findFirst().orElse(null);
            if (source == null) continue;

            List<String> targets = entities.stream()
                    .filter(e -> e.getTag().equals(rule.getTargetTag()))
                    .map(Entity::getText)
                    .collect(Collectors.toList());
            if (targets.isEmpty()) continue;

            return Relation.builder()
                    .type(rule.getRelationType())
                    .source(source.getText())
                    .targets(targets)
                    .build();
        }

        return null;
    }
}
