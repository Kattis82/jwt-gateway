package se.iths.kattis.jwtgateway.dto;

import java.util.List;

// representerar svaret som gatewayen skickar tillbaka till frontend
// token utelämnas eftersom den i stället ska skickas i en HttpOnly-cookie
public record LoginResponse(
        long expiresIn,
        String subject,
        List<String> roles
) {
}
