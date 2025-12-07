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

    public static Map<String, PatternRule> relationTypePatterns() {
        Map<String, PatternRule> ruleMap = new HashMap<>();

        ruleMap.put("located_in", PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(in|on|at|over)\\b"))
                .relationType("located_in")
                .sourceTag("condition")
                .targetTag("bodypart")
                .build());

        ruleMap.put("causes", PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(after the use of|after taking|caused)\\b"))
                .relationType("causes")
                .sourceTag("drug")
                .targetTag("symptom")
                .build());

        ruleMap.put("treats", PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(treated|for|relief of)\\b"))
                .relationType("treats")
                .sourceTag("drug")
                .targetTag("condition")
                .build());
        return ruleMap;
    }

}
