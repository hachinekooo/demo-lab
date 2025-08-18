package com.github.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GlobalConfAddDTO {
    @NotBlank
    private String confKey;

    @NotBlank
    private String confValue;

    @NotBlank
    private String confGroup;

    private String comment;
}