package com.example.clinicalextraction;

import com.example.clinicalextraction.entity.PatternRule;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Patterns {

    public static Map<String, PatternRule> relationTypePatterns() {

        Map<String, PatternRule> rules = new LinkedHashMap<>();

        rules.put("located_in",
                PatternRule.builder()
                        .relationType("located_in")
                        .triggerPattern(Pattern.compile("\\b(in|on|at|within)\\b", Pattern.CASE_INSENSITIVE))
                        .conjunctionBoundary(Pattern.compile("\\s*</bodypart>\\s+and\\s+<condition>", Pattern.CASE_INSENSITIVE))
                        .sourceTag("condition")
                        .targetTag("bodypart")
                        .allowMultipleTargets(true)
                        .splitOnNewSource(true)
                        .build()
        );

        rules.put("treats",
                PatternRule.builder()
                        .relationType("treats")
                        .triggerPattern(
                                Pattern.compile(
                                        "\\b(for|treated|relief of|managed|via|after taking)\\b",
                                        Pattern.CASE_INSENSITIVE
                                )
                        )
                        .sourceTag("drug")
                        .targetTag("condition")
                        .allowMultipleTargets(false)
                        .splitOnNewSource(false)
                        .build()
        );

        rules.put("causes",
                PatternRule.builder()
                        .relationType("causes")
                        .triggerPattern(
                                Pattern.compile(
                                        "\\b(caused|causing|after the use of|after use of|was reported)\\b",
                                        Pattern.CASE_INSENSITIVE
                                )
                        )
                        .sourceTag("drug")
                        .targetTag("symptom")
                        .allowMultipleTargets(true)
                        .splitOnNewSource(false)
                        .build()
        );

        return rules;
    }
}

