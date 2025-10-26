package com.isearch.text2vectorApp.model;

import jakarta.validation.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

public record DocumentRequest(@NotNull @NotEmpty List<String> texts) {

}
