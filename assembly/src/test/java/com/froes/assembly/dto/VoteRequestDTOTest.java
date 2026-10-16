package com.froes.assembly.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.froes.assembly.enums.VoteChoice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VoteRequestDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsOnlyUnaccentedPortugueseEnumChoices() throws Exception {
        VoteRequestDTO yes = objectMapper.readValue(
                "{\"surveyId\":10,\"electorId\":20,\"approval\":\"SIM\"}", VoteRequestDTO.class);
        VoteRequestDTO no = objectMapper.readValue(
                "{\"surveyId\":10,\"electorId\":20,\"approval\":\"NAO\"}", VoteRequestDTO.class);
        assertEquals(VoteChoice.SIM, yes.approval());
        assertEquals(VoteChoice.NAO, no.approval());
    }

    @Test
    void rejectsUnsupportedVoteChoice() {
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(
                "{\"surveyId\":10,\"electorId\":20,\"approval\":\"MAYBE\"}", VoteRequestDTO.class));
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(
                "{\"surveyId\":10,\"electorId\":20,\"approval\":\"NÃO\"}", VoteRequestDTO.class));
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(
                "{\"surveyId\":10,\"electorId\":20,\"approval\":true}", VoteRequestDTO.class));
    }
}
