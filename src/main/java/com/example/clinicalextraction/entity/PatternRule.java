package com.example.clinicalextraction.entity;

import lombok.Builder;
import lombok.Data;

import java.util.regex.Pattern;

@Data
@Builder
public class PatternRule {

    private Pattern triggerPattern;

    private String relationType;

    private String sourceTag;

    private String targetTag;

    private boolean allowMultipleTargets;

    private boolean splitOnNewSource;

    private Pattern conjunctionBoundary;
}
