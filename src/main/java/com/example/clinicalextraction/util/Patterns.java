package com.example.clinicalextraction.util;

import com.example.clinicalextraction.entity.Direction;
import com.example.clinicalextraction.entity.GroupingStrategy;
import com.example.clinicalextraction.entity.PatternRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Patterns {

    public static Map<String, PatternRule> relationTypePatterns() {
        Map<String, PatternRule> ruleMap = new HashMap<>();

        ruleMap.put("located_in", PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(in|on|at|over)\\b"))
                .relationType("located_in")
                .sourceTag("condition")
                .targetTag("bodypart")
                .direction(Direction.RIGHT)
                .groupingStrategy(GroupingStrategy.SCOPE_UNTIL_NEXT)

                .build());

        ruleMap.put("causes", PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(after the use of|after taking|caused)\\b"))
                .relationType("causes")
                .sourceTag("drug")
                .targetTag("symptom")
                .direction(Direction.RIGHT)
                .groupingStrategy(GroupingStrategy.SCOPE_UNTIL_NEXT)
                .maxTokenDistance(100)
                .build());

        ruleMap.put("treats", PatternRule.builder()
                .triggerPattern(Pattern.compile("\\b(treated|for|relief of)\\b"))
                .relationType("treats")
                .sourceTag("drug")
                .targetTag("condition")
                .direction(Direction.RIGHT)
                .groupingStrategy(GroupingStrategy.SCOPE_UNTIL_NEXT)
                .maxTokenDistance(100)
                .build());
        return ruleMap;
    }

}
