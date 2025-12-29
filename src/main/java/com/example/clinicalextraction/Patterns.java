package com.example.clinicalextraction;

import com.example.clinicalextraction.dto.Direction;
import com.example.clinicalextraction.entity.PatternRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Patterns {

    public static Map<String, List<PatternRule>> relationTypePatterns() {
        Map<String, List<PatternRule>> ruleMap = new HashMap<>();

        ruleMap.put("located_in", List.of(PatternRule.builder()
                        .triggerPattern(Pattern.compile("\\b(radiating to|radiates to|radiate to|in|on)\\b"))
                        .relationType("located_in")
                        .sourceTag("symptom")
                        .targetTag("bodypart")
                        .direction(Direction.LEFT)
                .build(),
                PatternRule.builder()
                        .triggerPattern(Pattern.compile("\\b(in|on|at|over)\\b"))
                        .relationType("located_in")
                        .sourceTag("condition")
                        .targetTag("bodypart")
                        .direction(Direction.LEFT)
                        .build(),
                PatternRule.builder()
                        .triggerPattern(Pattern.compile("\\b(in|with)\\b"))
                        .relationType("located_in")
                        .sourceTag("bodypart")
                        .targetTag("symptom")
                        .direction(Direction.RIGHT)
                        .build()));

        ruleMap.put("causes", List.of(PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(confirmed by)\\b"))
                .relationType("located_in")
                .sourceTag("condition")
                .targetTag("symptom")
                .build(),
                PatternRule.builder()
                        .triggerPattern(Pattern.compile("\\b(after the use of|after taking|caused)\\b"))
                        .relationType("causes")
                        .sourceTag("drug")
                        .targetTag("symptom")
                        .direction(Direction.RIGHT)
                        .build()));

        ruleMap.put("revealed_by", List.of(PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(revealed)\\b"))
                .relationType("revealed_by")
                .sourceTag("condition")
                .targetTag("event")
                .direction(Direction.LEFT)
                .build()));

        ruleMap.put("detected_by", List.of(PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(detected during)\\b"))
                .relationType("detected_by")
                .sourceTag("symptom")
                .targetTag("event")
                .direction(Direction.LEFT)
                .build()));

        ruleMap.put("treats", List.of(PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(treated|for|relief of|after taking)\\b"))
                .relationType("treats")
                .sourceTag("drug")
                .targetTag("condition")
                .direction(Direction.RIGHT)
                .build()));

        return ruleMap;
    }

}
