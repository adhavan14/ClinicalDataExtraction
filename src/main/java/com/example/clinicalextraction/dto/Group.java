package com.example.clinicalextraction.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
public class Group {

    private Long groupId;

    private String reference;

    private List<Entity> entities;

    private List<Relation> suggestedRelation;

}
