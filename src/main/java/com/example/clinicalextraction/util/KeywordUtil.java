package com.example.clinicalextraction.util;

import java.util.Map;
import java.util.Set;

public class KeywordUtil {

    public static final Map<String, Double> TREAT_KEYWORDS = Map.ofEntries(

            Map.entry("prescribe", 0.7),
            Map.entry("treat", 0.7),
            Map.entry("manage", 0.7),
            Map.entry("initiate", 0.7),
            Map.entry("give", 0.7),

            Map.entry("start", 0.7),
            Map.entry("restart", 0.7),
            Map.entry("receive", 0.7),

            Map.entry("take", 0.1),

            Map.entry("treatment", 0.7),
            Map.entry("prophylaxis", 0.7)
    );

    public static final Set<String> ADVERSE_EFFECT = Set.of(
            "develop",
            "cause",
            "induce",
            "trigger",
            "lead",
            "associate",
            "result",
            "produce",
            "experience",
            "report",
            "complain",
            "present");

    public static double getWeight(String lemma) {
        return TREAT_KEYWORDS.getOrDefault(lemma, 0.0);
    }
}
