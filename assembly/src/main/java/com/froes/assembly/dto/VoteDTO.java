package com.froes.assembly.dto;

public record VoteDTO(Long id, Long surveyId, Long electorId, Boolean approval, String choice) {}
