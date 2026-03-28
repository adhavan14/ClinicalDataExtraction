package com.example.clinicalextraction.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CandidatePair {

    private String drug;

    private String condition;

    private Integer drugSentenceIndex;

    private Integer conditionSentenceIndex;

    private String proximityLevel;

    private Integer distance;

    private List<String> sentenceLemmas;

    private List<DependencyLink> dependencyLinks;
}
