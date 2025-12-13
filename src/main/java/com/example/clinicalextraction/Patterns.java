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
                .triggerPattern(Pattern.compile("\\b(in|on|at|over|localized)\\b"))
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

        ruleMap.put("triggered_by", PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(after|during)\\b", Pattern.CASE_INSENSITIVE))
                .relationType("triggered_by")
                .sourceTag("symptom")
                .targetTag("event")
                .build());

        ruleMap.put("treats_condition", PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(using|treated|for|relief of|as needed|started|rx)\\b", Pattern.CASE_INSENSITIVE))
                .relationType("treats")
                .sourceTag("drug")
                .targetTag("condition")
                .build());

        ruleMap.put("treats_symptom", PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(using|treated|for|relief of|as needed|started|rx)\\b", Pattern.CASE_INSENSITIVE))
                .relationType("treats")
                .sourceTag("drug")
                .targetTag("symptom")
                .build());
        return ruleMap;
    }

}
