package com.example.clinicalextraction.service;

import com.example.clinicalextraction.dto.Direction;
import com.example.clinicalextraction.dto.Entity;
import com.example.clinicalextraction.dto.Relation;
import com.example.clinicalextraction.entity.PatternRule;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;

@Component
public class RelationExtractionService {

    public List<Relation> extractRelations(
            String text,
            List<Entity> entities,
            Map<String, List<PatternRule>> ruleMap
    ) {

        List<Relation> relations = new ArrayList<>();

        for (List<PatternRule> rules : ruleMap.values()) {
            for (PatternRule rule : rules) {

                Matcher matcher = rule.getTriggerPattern().matcher(text);

                while (matcher.find()) {
                    int triggerStart = matcher.start();
                    int triggerEnd = matcher.end();

                    Optional<Entity> sourceOpt =
                            findSourceEntity(
                                    entities,
                                    triggerStart,
                                    triggerEnd,
                                    rule
                            );

                    if (sourceOpt.isEmpty()) continue;

                    Entity source = sourceOpt.get();

                    List<Entity> targets =
                            findTargetsInClosedScope(
                                    entities,
                                    rule.getDirection() == Direction.LEFT
                                            ? triggerEnd
                                            : 0,
                                    rule.getTargetTag(),
                                    source
                            );

                    if (targets.isEmpty()) continue;

                    relations.add(Relation.builder()
                            .type(rule.getRelationType())
                            .source(source.getText())
                            .targets(targets.stream()
                                    .map(Entity::getText)
                                    .toList())
                            .build());
                }
            }
        }
        return relations;
    }

    private Optional<Entity> findSourceEntity(
            List<Entity> entities,
            int triggerStart,
            int triggerEnd,
            PatternRule rule
    ) {

        return rule.getDirection() == Direction.LEFT
                ? findNearestLeftEntity(entities, triggerStart, rule.getSourceTag())
                : findNearestRightEntity(entities, triggerEnd, rule.getSourceTag());
    }

    private Optional<Entity> findNearestLeftEntity(
            List<Entity> entities,
            int triggerPos,
            String requiredTag
    ) {
        Entity best = null;

        for (Entity e : entities) {
            if (!e.getTag().equals(requiredTag)) continue;
            if (e.getEnd() > triggerPos) continue;

            if (best == null || e.getEnd() > best.getEnd()) {
                best = e;
            }
        }
        return Optional.ofNullable(best);
    }

    private Optional<Entity> findNearestRightEntity(
            List<Entity> entities,
            int triggerPos,
            String requiredTag
    ) {
        for (Entity e : entities) {
            if (e.getStart() >= triggerPos && e.getTag().equals(requiredTag)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    private List<Entity> findTargetsInClosedScope(
            List<Entity> entities,
            int scopeStart,
            String requiredTag,
            Entity source
    ) {

        List<Entity> targets = new ArrayList<>();
        Entity lastAdded = null;

        for (Entity e : entities) {

            if (e.getStart() < scopeStart) continue;
            if (e == source) continue;

            if (lastAdded != null && isNewClauseHead(e, lastAdded)) {
                break;
            }

            if (e.getTag().equals(requiredTag)) {
                targets.add(e);
                lastAdded = e;
            }
        }
        return targets;
    }

    private boolean isNewClauseHead(Entity current, Entity previous) {

        if (current.getTag().equals(previous.getTag())
                && current.getStart() - previous.getEnd() <= 10) {
            return false;
        }

        return current.getTag().equals("symptom")
                || current.getTag().equals("condition")
                || current.getTag().equals("drug");
    }
}

