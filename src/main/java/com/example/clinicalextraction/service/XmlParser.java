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
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public List<Group> parseChunk(String chunk) throws SAXException, IOException {

        ClinicalXMLHandler clinicalXMLHandler = new ClinicalXMLHandler();

        String wrappedChunk = "<root>" + chunk + "</root>";

        saxParser.parse(new InputSource(new StringReader(wrappedChunk)), clinicalXMLHandler);

        List<String> reference = extractReference(chunk);

        return buildGroups(clinicalXMLHandler.entities, reference, chunk);
    }

    private List<Group> buildGroups(List<Entity> entities, List<String> references, String chunk) {

        List<Group> groups = new ArrayList<>();
        Group currentGroup = newGroup();

        RelationContext ctx = new RelationContext();
        int refIndex = 0;

        for (Entity entity : entities) {

            PatternRule rule = findRuleForEntity(entity);

            if (rule != null) {

                // 🔥 SPLIT CONDITION (TEXT-AWARE)
                if (entity.getTag().equals(rule.getSourceTag())
                        && ctx.rule != null
                        && ctx.rule.equals(rule)
                        && rule.isSplitOnNewSource()
                        && ctx.hasTargets()) {

                    // check conjunction boundary in raw text span
                    String betweenText = extractTextBetween(
                            ctx.lastTargetEnd,
                            entity.getStart(),
                            chunk
                    );

                    System.out.println("Between Text: '" + betweenText + "'");

                    if (rule.getConjunctionBoundary()
                            .matcher(betweenText)
                            .find()) {

                        finalizeGroup(currentGroup, references, refIndex++, groups);
                        currentGroup = newGroup();
                        ctx.reset();
                    }
                }

                if (entity.getTag().equals(rule.getSourceTag())) {
                    ctx.rule = rule;
                    ctx.source = entity;
                }

                if (entity.getTag().equals(rule.getTargetTag())) {
                    ctx.targets.add(entity);
                    ctx.lastTargetEnd = entity.getEnd();
                }
            }

            currentGroup.getEntities().add(entity);
        }


        if (!currentGroup.getEntities().isEmpty() && refIndex < references.size()) {
            finalizeGroup(currentGroup, references, refIndex, groups);
        }

        return groups;
    }

    private String extractTextBetween(int start, int end, String reference) {
        if (start < 0 || end <= start) return "";
        return reference.substring(start, end);
    }

    private PatternRule findRuleForEntity(Entity entity) {
        return Patterns.relationTypePatterns()
                .values()
                .stream()
                .filter(rule ->
                        entity.getTag().equals(rule.getSourceTag())
                                || entity.getTag().equals(rule.getTargetTag())
                )
                .findFirst()
                .orElse(null);
    }

    private static class RelationContext {

        PatternRule rule;
        Entity source;
        List<Entity> targets = new ArrayList<>();
        int lastTargetEnd = -1;

        boolean hasTargets() {
            return !targets.isEmpty();
        }

        void reset() {
            rule = null;
            source = null;
            targets.clear();
            lastTargetEnd = -1;
        }
    }


    private Group newGroup() {
        Group g = Group.builder().build();
        g.setEntities(new ArrayList<>());
        return g;
    }

    private void finalizeGroup(Group group, List<String> references, int index, List<Group> groups) {
        group.setGroupId(groups.size() + 1L);
        group.setReference(references.get(index).replaceAll("\\.$", ""));
        groups.add(group);
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
