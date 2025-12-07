package com.example.clinicalextraction.dto;


import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class Entity {

    private String tag;

    private String text;

    private int start;

    private int end;
}
