package com.github.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GlobalConfUpdateDTO {

    @NotBlank
    private String confKey;

    @NotBlank
    private String confValue;
}