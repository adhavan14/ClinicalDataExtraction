package com.example.clinicalextraction.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RelationEvidence {

    private String drug;
    private String condition;

    private String relationType;
    private double confidence;

    private List<String> keywords;
    private List<String> dependencyPaths;
    private String pattern;

    private int distance;
    private String proximity;
}
