package com.example.clinicalextraction.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;


@Data
@Builder
@ToString
public class RelationKey {
    private int sourceStart;
    private int targetStart;
    private String relationType;
}
