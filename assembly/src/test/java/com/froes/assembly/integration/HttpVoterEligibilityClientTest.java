package com.froes.assembly.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.froes.assembly.enums.ElectorStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpVoterEligibilityClientTest {

    private static final String CPF = "67953008033";
    private static final String URL = "https://eligibility.example/users/" + CPF;

    private MockRestServiceServer server;
    private HttpVoterEligibilityClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://eligibility.example");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpVoterEligibilityClient(builder.build(), new ObjectMapper());
    }

    @Test
    void returnsEligibleStatus() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("ABLE_TO_VOTE", MediaType.TEXT_PLAIN));

        assertEquals(ElectorStatusEnum.ABLE_TO_VOTE, client.checkEligibility(CPF));
        server.verify();
    }

    @Test
    void returnsIneligibleStatusFromJson() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"status\":\"UNABLE_TO_VOTE\"}", MediaType.APPLICATION_JSON));

        assertEquals(ElectorStatusEnum.UNABLE_TO_VOTE, client.checkEligibility(CPF));
        server.verify();
    }

    @Test
    void mapsNotFoundToExternalVoterNotFound() {
        server.expect(requestTo(URL)).andRespond(withResourceNotFound());

        assertThrows(ExternalVoterNotFoundException.class, () -> client.checkEligibility(CPF));
        server.verify();
    }

    @Test
    void mapsServiceFailureToUnavailableException() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThrows(ExternalEligibilityServiceException.class, () -> client.checkEligibility(CPF));
        server.verify();
    }

    @Test
    void rejectsUnexpectedStatusBody() {
        server.expect(requestTo(URL)).andRespond(withSuccess("UNKNOWN", MediaType.TEXT_PLAIN));

        assertThrows(InvalidEligibilityResponseException.class, () -> client.checkEligibility(CPF));
        server.verify();
    }
}
