package com.example.clinicalextraction.entity;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class DependencyLink {

    private String source;

    private String relation;

    private String target;
}
