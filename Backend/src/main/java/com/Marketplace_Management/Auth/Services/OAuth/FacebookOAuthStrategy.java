package com.Marketplace_Management.Auth.Services.OAuth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Constants.OAuthProvider;
import com.Marketplace_Management.Shared.Utils.Http.RestClients;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Facebook Login: the credential is a user access token from the JS SDK (FB.login).
 * Unlike Google there is no offline-verifiable ID token, so the Graph API is called:
 *  1. debug_token (with the app access token "appId|appSecret"): the token is valid AND was issued
 *     for OUR app — otherwise a token obtained by any other Facebook app could log in here.
 *  2. /me (with appsecret_proof): id, name, email, picture of that user.
 */
@Component("facebook")
public class FacebookOAuthStrategy implements OAuthStrategy {
    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuthStrategy.class);

    private final RestClient restClient;
    private final String appId;
    private final String appSecret;

    public FacebookOAuthStrategy(
            RestClient.Builder builder,
            @Value("${third-party.facebook.app-id}") String appId,
            @Value("${third-party.facebook.app-secret}") String appSecret,
            @Value("${third-party.facebook.graph-url:https://graph.facebook.com}") String graphUrl) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.restClient = RestClients.withTimeouts(builder, graphUrl, Duration.ofSeconds(5), Duration.ofSeconds(10)).build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.FACEBOOK;
    }

    @Override
    public OAuthUserInfo verify(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthenticationException(Message.OAUTH_TOKEN_INVALID) {};
        }

        DebugToken token = call(() -> restClient.get()
                .uri(uri -> uri.path("/debug_token")
                        .queryParam("input_token", accessToken)
                        .queryParam("access_token", appId + "|" + appSecret)
                        .build())
                .retrieve()
                .body(DebugTokenResponse.class)).data();

        if (token == null || !token.valid() || !appId.equals(token.appId()) || token.userId() == null) {
            throw new AuthenticationException(Message.OAUTH_TOKEN_INVALID) {};
        }

        Me me = call(() -> restClient.get()
                .uri(uri -> uri.path("/me")
                        .queryParam("fields", "id,name,email,picture.width(256).height(256)")
                        .queryParam("access_token", accessToken)
                        .queryParam("appsecret_proof", appSecretProof(accessToken))
                        .build())
                .retrieve()
                .body(Me.class));

        if (me == null || !token.userId().equals(me.id())) {
            throw new AuthenticationException(Message.OAUTH_TOKEN_INVALID) {};
        }

        String picture = me.picture() != null && me.picture().data() != null ? me.picture().data().url() : null;
        // Facebook only returns confirmed email addresses
        return new OAuthUserInfo(OAuthProvider.FACEBOOK, me.id(), me.email(), me.email() != null, me.name(), picture);
    }

    /** 4xx from Graph = bad/expired token (401); 5xx or network failure = provider unavailable (503). */
    private <T> T call(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                logger.warn("Facebook rejected the token: {}", e.getResponseBodyAsString());
                throw new AuthenticationException(Message.OAUTH_TOKEN_INVALID) {};
            }
            logger.error("Facebook Graph API error {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, Message.OAUTH_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            logger.error("Facebook Graph API unreachable: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, Message.OAUTH_UNAVAILABLE);
        }
    }

    /** HMAC-SHA256 of the access token keyed with the app secret (Graph API "appsecret_proof"). */
    private String appSecretProof(String accessToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    // ── Graph API payloads ──────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DebugTokenResponse(DebugToken data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DebugToken(
            @JsonProperty("app_id") String appId,
            @JsonProperty("is_valid") boolean valid,
            @JsonProperty("user_id") String userId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Me(String id, String name, String email, Picture picture) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Picture(PictureData data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PictureData(String url) {}
}
