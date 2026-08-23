package com.jam.agent.agent.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class ModelFailureCategoryTest {

    @Test
    void classifiesNetworkFailureAsTimeoutFailover() {
        assertEquals(
                ModelFailureCategory.TIMEOUT,
                ModelFailureCategory.classify(new ResourceAccessException("connection reset")));
    }

    @Test
    void classifiesAuthenticationFailureAsPermanent() {
        Throwable error = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8);

        assertEquals(ModelFailureCategory.AUTH, ModelFailureCategory.classify(error));
    }

    @Test
    void classifiesOverloadAsFailover() {
        Throwable error = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8);

        assertEquals(ModelFailureCategory.RATE_LIMIT, ModelFailureCategory.classify(error));
    }
}
