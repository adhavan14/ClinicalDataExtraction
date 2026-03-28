package com.example.clinicalextraction.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SentenceData {

    private String sentence;


    private List<String> conditions;

    private List<String> drugs;


}
