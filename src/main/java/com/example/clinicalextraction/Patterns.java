package com.example.clinicalextraction;

import com.example.clinicalextraction.entity.PatternRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Patterns {

    public static List<String> getTriggersForGroup() {
        return List.of(
                "condition"
        );
    }

    public static Map<String, List<PatternRule>> relationTypePatterns() {
        Map<String, List<PatternRule>> ruleMap = new HashMap<>();

        ruleMap.put("located_in", List.of(PatternRule.builder()
                        .triggerPattern(Pattern.compile("\\b(radiating to|radiates to|radiate to|in|on)\\b"))
                        .relationType("located_in")
                        .sourceTag("symptom")
                        .targetTag("bodypart")
                .build(),
                PatternRule.builder()
                        .triggerPattern(Pattern.compile("\\b(in|on|at|over)\\b"))
                        .relationType("located_in")
                        .sourceTag("condition")
                        .targetTag("bodypart")
                        .build(),
                PatternRule.builder()
                        .triggerPattern(Pattern.compile("\\b(in|with)\\b"))
                        .relationType("located_in")
                        .sourceTag("bodypart")
                        .targetTag("symptom")
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
                        .build()));

        ruleMap.put("revealed_by", List.of(PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(revealed)\\b"))
                .relationType("revealed_by")
                .sourceTag("condition")
                .targetTag("event")
                .build()));

        ruleMap.put("detected_by", List.of(PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(detected during)\\b"))
                .relationType("detected_by")
                .sourceTag("symptom")
                .targetTag("event")
                .build()));

        ruleMap.put("treats", List.of(PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(treated|for|relief of)\\b"))
                .relationType("treats")
                .sourceTag("drug")
                .targetTag("condition")
                .build()));

        return ruleMap;
    }

}
