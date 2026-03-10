package com.example.clinicalextraction.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
public class Relation {

    private String type;

    private List<String> source;

    private List<String> targets;
}
