package com.froes.assembly.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.froes.assembly.enums.ElectorStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpVoterEligibilityClient implements VoterEligibilityClient {

    private final RestClient eligibilityRestClient;
    private final ObjectMapper objectMapper;

    @Override
    public ElectorStatusEnum checkEligibility(String cpf) {
        try {
            String response = eligibilityRestClient.get()
                    .uri("/users/{cpf}", cpf)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        if (clientResponse.getStatusCode().value() == 404) {
                            throw new ExternalVoterNotFoundException();
                        }
                        throw new ExternalEligibilityServiceException(
                                "O serviço externo de elegibilidade retornou HTTP "
                                        + clientResponse.getStatusCode().value(),
                                null);
                    })
                    .body(String.class);
            return parseStatus(response);
        } catch (ExternalVoterNotFoundException | ExternalEligibilityServiceException
                 | InvalidEligibilityResponseException exception) {
            throw exception;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ExternalVoterNotFoundException();
        } catch (RestClientException exception) {
            log.warn("A requisição de elegibilidade do eleitor ao serviço externo falhou");
            throw new ExternalEligibilityServiceException(
                    "O serviço externo de elegibilidade está indisponível", exception);
        }
    }

    private ElectorStatusEnum parseStatus(String response) {
        if (response == null || response.isBlank()) {
            throw new InvalidEligibilityResponseException();
        }

        String status = response.trim();
        if (status.startsWith("{")) {
            try {
                JsonNode json = objectMapper.readTree(status);
                status = json.path("status").asText();
            } catch (IOException exception) {
                throw new InvalidEligibilityResponseException();
            }
        } else if (status.startsWith("\"")) {
            try {
                status = objectMapper.readValue(status, String.class);
            } catch (IOException exception) {
                throw new InvalidEligibilityResponseException();
            }
        }

        try {
            return ElectorStatusEnum.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidEligibilityResponseException();
        }
    }
}
