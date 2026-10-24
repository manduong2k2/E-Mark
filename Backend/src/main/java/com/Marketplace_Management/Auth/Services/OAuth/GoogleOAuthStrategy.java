package com.Marketplace_Management.Auth.Services.OAuth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Constants.OAuthProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Google Sign-In: the credential is an ID token (JWT), verified offline.
 * Google's public keys are downloaded once and cached by the verifier, then each token is checked
 * locally: signature, issuer, expiry and audience (our OAuth client ID).
 */
@Component("google")
public class GoogleOAuthStrategy implements OAuthStrategy {
    private final GoogleIdTokenVerifier verifier;

    public GoogleOAuthStrategy(@Value("${third-party.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String idToken) {
        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (IllegalArgumentException e) {
            token = null;   // malformed JWT
        } catch (GeneralSecurityException | IOException e) {
            // Could not fetch Google's public keys
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, Message.OAUTH_UNAVAILABLE, e);
        }

        if (token == null) {
            throw new AuthenticationException(Message.OAUTH_TOKEN_INVALID) {};
        }

        GoogleIdToken.Payload payload = token.getPayload();
        return new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                payload.getSubject(),
                payload.getEmail(),
                Boolean.TRUE.equals(payload.getEmailVerified()),
                (String) payload.get("name"),
                (String) payload.get("picture"));
    }
}
