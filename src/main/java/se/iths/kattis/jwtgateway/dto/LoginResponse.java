package se.iths.kattis.jwtgateway.dto;

import java.util.List;

/**
 * Svaret till frontend efter inloggning: giltighetstid i sekunder, användarnamn (subject)
 * och roller. Själva JWT skickas aldrig här, utan bara i HttpOnly-cookien.
 */
public record LoginResponse(
        long expiresIn,
        String subject,
        List<String> roles
) {
}
