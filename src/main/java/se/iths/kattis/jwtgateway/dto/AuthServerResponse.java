package se.iths.kattis.jwtgateway.dto;


import java.util.List;

/**
 * Svaret från Auth Server vid lyckad inloggning. Innehåller JWT (accessToken),
 * giltighetstid i sekunder (expiresIn), användarnamn (subject) och roller.
 * Gateway läser token härifrån och sparar den i en HttpOnly-cookie.
 */
public record AuthServerResponse(
        String accessToken,
        long expiresIn,
        String subject,
        List<String> roles
) {
}
