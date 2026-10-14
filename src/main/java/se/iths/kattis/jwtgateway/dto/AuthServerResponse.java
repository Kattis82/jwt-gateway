package se.iths.kattis.jwtgateway.dto;


import java.util.List;

// representerar hela svaret från Auth Server vid lyckad inloggning
// JWT-token finns med här eftersom gatewayen behöver den för att skapa cookien
public record AuthServerResponse(
        String accessToken,
        long expiresIn,
        String subject,
        List<String> roles
) {
}
