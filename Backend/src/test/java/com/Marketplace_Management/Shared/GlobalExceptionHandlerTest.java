package com.Marketplace_Management.Shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Marketplace_Management.Shared.Errors.GlobalExceptionHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class GlobalExceptionHandlerTest {
    private static final String GENERIC_MESSAGE = "An unexpected error occurred. Please try again later.";

    @Test
    void localEnvironmentReturnsExceptionDetailsInHttpResponse() throws Exception {
        JsonNode body = request(new MockEnvironment().withProperty("spring.application.environment", "local"));

        assertEquals("Unexpected detail", body.path("message").asString());
        assertEquals(NullPointerException.class.getName(), body.path("className").asString());
        assertEquals("Cause detail", body.path("cause").asString());
        assertEquals(2, body.path("fullTrace").size());
        assertEquals(1, body.path("appTrace").size());
        assertTrue(body.path("appTrace").path(0).asString().startsWith("com.Marketplace_Management."));
    }

    @Test
    void localEnvironmentIsCaseInsensitive() throws Exception {
        JsonNode body = request(new MockEnvironment().withProperty("spring.application.environment", "LOCAL"));
        assertTrue(body.has("fullTrace"));
    }

    @Test
    void nonLocalEnvironmentsHideDetailsByDefault() throws Exception {
        for (String environment : new String[] { "production", "prod", "dev", "test" }) {
            assertGeneric(request(new MockEnvironment().withProperty("spring.application.environment", environment)));
        }
    }

    @Test
    void missingEnvironmentHidesDetailsByDefault() throws Exception {
        assertGeneric(request(new MockEnvironment()));
    }

    @Test
    void legacyDebugFalseDoesNotHideLocalDetails() throws Exception {
        JsonNode body = request(new MockEnvironment()
                .withProperty("spring.application.environment", "local").withProperty("app.debug", "false"));
        assertEquals("Unexpected detail", body.path("message").asString());
        assertTrue(body.has("fullTrace"));
    }

    @Test
    void legacyDebugTrueDoesNotExposeNonLocalDetails() throws Exception {
        assertGeneric(request(new MockEnvironment()
                .withProperty("spring.application.environment", "production").withProperty("app.debug", "true")));
    }

    private JsonNode request(MockEnvironment environment) throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler(environment)).build();
        var response = mvc.perform(get("/test/unexpected")).andReturn().getResponse();
        assertEquals(500, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertEquals(500, body.path("status").asInt());
        return body;
    }

    private void assertGeneric(JsonNode body) {
        assertEquals(GENERIC_MESSAGE, body.path("message").asString());
        for (String field : new String[] { "className", "cause", "fullTrace", "appTrace" }) {
            assertFalse(body.has(field), field + " must not be exposed");
        }
    }

    @RestController
    static class FailingController {
        @GetMapping("/test/unexpected")
        public void fail() {
            var exception = new NullPointerException("Unexpected detail");
            exception.initCause(new IOException("Cause detail"));
            exception.setStackTrace(new StackTraceElement[] {
                    new StackTraceElement("com.Marketplace_Management.Catalog.Services.ProductService", "create", "ProductService.java", 42),
                    new StackTraceElement("org.example.Library", "execute", "Library.java", 10)
            });
            throw exception;
        }
    }
}
