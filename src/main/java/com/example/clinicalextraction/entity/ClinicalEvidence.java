package com.example.clinicalextraction.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class ClinicalEvidence {

    private List<SentenceData> sentenceDataList;

    private Set<CandidatePair> candidatePairs;

    private List<RelationEvidence> relationEvidences;
}
