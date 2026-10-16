package com.froes.assembly.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.froes.assembly.resource.handler.ResourceExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResourceExceptionHandlerTest {

    private final ResourceExceptionHandler handler = new ResourceExceptionHandler();

    @Test
    void returnsStandardConflictBody() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/votes");
        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate vote"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("CONFLICT", response.getBody().getError());
        assertEquals("Duplicate vote", response.getBody().getMessage());
        assertEquals("/api/v1/votes", response.getBody().getPath());
    }

    @Test
    void malformedInputDoesNotExposeInternalTrace() throws JsonProcessingException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/votes");
        var response = handler.handleUnreadableBody(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("BAD_REQUEST", response.getBody().getError());
        assertFalse(new ObjectMapper().findAndRegisterModules()
                .writeValueAsString(response.getBody()).contains("trace"));
    }

    @Test
    void returnsNotFoundForRemovedRoutes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/survey");
        var response = handler.handleResourceNotFound(
                new NoResourceFoundException(HttpMethod.GET, "survey"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("/survey", response.getBody().getPath());
    }
}
