package se.iths.kattis.jwtgateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.kattis.jwtgateway.service.GatewayProxyService;

// definierar gatewayens publika API för frontend
@RestController
@RequestMapping("/gateway")
public class GatewayController {

    // används för att vidarebefordra anrop till mikrotjänsterna
    private final GatewayProxyService proxyService;

    public GatewayController(GatewayProxyService proxyService) {
        this.proxyService = proxyService;
    }

    // tar emot inloggningsuppgifter från frontend och skickar dem till Auth Server
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestBody String requestBody,
            HttpServletRequest request) {

        // skickar inloggningsuppgifterna vidare till Auth Server
        // Gatewayen hanterar JWT-token och returnerar den som en HttpOnly-cookie
        return proxyService.loginWithCookie(
                requestBody,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    // tar emot registreringsuppgifter från frontend och skickar dem till Auth Server
    @PostMapping(
            value = "/users/register",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> register(
            @RequestBody String requestBody,
            HttpServletRequest request) {

        // hämtar eventuell Authorization-header från det inkommande anropet
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // skickar registreringsuppgifterna vidare till Auth Server
        return proxyService.forwardPost(
                "/users/register",
                requestBody,
                authorization,
                request.getContentType()
        );
    }


    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return proxyService.logout();
    }


    // frontend anropar denna endpoint när appen startar om eller laddas om
    @GetMapping("/auth/me")
    public ResponseEntity<String> getCurrentUser(
            // läser JWT-token från gatewayens HttpOnly-cookie
            // required = false gör att vi själva kan hantera saknad cookie
            @CookieValue(name = "access_token", required = false)
            String accessToken) {

        // kontrollerar att cookien innehåller en token
        ResponseEntity<String> unauthorized =
                unauthorizedIfMissing(accessToken);

        // om cookien saknas skickas 401 Unauthorized till frontend
        if (unauthorized != null) {
            return unauthorized;
        }

        // gör om token till formatet som Auth Server förväntar sig
        String authorization = getAuthorizationFromCookie(accessToken);

        // skickar token vidare till Auth Server och returnerar svaret
        return proxyService.getCurrentUser(authorization);
    }


    // hämtar alla produkter från Product Service
    // Metoden är publik och kräver därför ingen inloggning eller cookie
    @GetMapping("/products")
    public ResponseEntity<String> getProducts() {

        // skickar anropet vidare till Product Service utan JWT-token
        return proxyService.getProducts(null);
    }


    // hämtar en specifik produkt med hjälp av dess ID
    @GetMapping("/products/{id}")
    public ResponseEntity<String> getProductById(
            @PathVariable String id,

            // hämtar JWT-token från webbläsarens HttpOnly-cookie
            // required = false gör att vi själva kan hantera saknad cookie
            @CookieValue(name = "access_token", required = false)
            String accessToken) {


        // kontrollerar att användaren har en JWT-token i cookien.
        ResponseEntity<String> unauthorized =
                unauthorizedIfMissing(accessToken);

        // avbryter anropet om cookien saknas.
        if (unauthorized != null) {
            return unauthorized;
        }

        // gör om cookiens token till en Bearer-token
        String authorization = getAuthorizationFromCookie(accessToken);


        // skickar vidare produktens ID och token till Product Service
        return proxyService.getProductById(id, authorization);
    }


    // skapar en ny produkt
    // JWT-token hämtas från den HttpOnly-cookie som skapades vid inloggning
    @PostMapping(value = "/products",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createProduct(
            @RequestBody String requestBody,
            @CookieValue(name = "access_token", required = false)
            String accessToken, HttpServletRequest request) {

        // kontrollerar att användaren har en JWT-token i cookien
        ResponseEntity<String> unauthorized =
                unauthorizedIfMissing(accessToken);

        // avbryter anropet om cookien saknas
        if (unauthorized != null) {
            return unauthorized;
        }

        // gör om token från cookien till en Bearer-token
        String authorization = getAuthorizationFromCookie(accessToken);

        // skickar produktdata och token vidare till Product Service
        return proxyService.createProduct(
                requestBody,
                authorization,
                request.getContentType()
        );
    }

    // tar bort en produkt med hjälp av dess ID
    // JWT-token hämtas från den HttpOnly-cookie som skapades vid inloggning
    @DeleteMapping("/products/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable String id,
            @CookieValue(name = "access_token", required = false)
            String accessToken) {

        // kontrollerar att användaren har en JWT-token i cookien
        ResponseEntity<String> unauthorized =
                unauthorizedIfMissing(accessToken);

        // avbryter anropet om cookien saknas
        if (unauthorized != null) {
            return unauthorized;
        }

        // Gör om token från cookien till en Bearer-token.
        String authorization = getAuthorizationFromCookie(accessToken);

        // skickar vidare produktens ID och token
        return proxyService.deleteProduct(id, authorization);
    }


    // skapar en order
    // JWT-token hämtas från den HttpOnly-cookie som skapades vid inloggning
    @PostMapping(value = "/orders",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createOrder(
            @RequestBody String requestBody,
            @CookieValue(name = "access_token", required = false)
            String accessToken,
            HttpServletRequest request) {

        // kontrollerar att användaren har en JWT-token i cookien
        ResponseEntity<String> unauthorized =
                unauthorizedIfMissing(accessToken);

        // avbryter anropet om cookien saknas
        if (unauthorized != null) {
            return unauthorized;
        }

        // Gör om token från cookien till en Bearer-token.
        String authorization = getAuthorizationFromCookie(accessToken);

        // skickar orderns JSON-data och headers vidare till Order Service
        return proxyService.createOrder(
                requestBody,
                authorization,
                request.getContentType()
        );
    }

    // hjälpmetoder

    // hämtar JWT-token från cookien och gör om den till en Bearer-token
    // om cookien saknas returneras null så att vi inte skickar "Bearer null"
    private String getAuthorizationFromCookie(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }

        return "Bearer " + accessToken;
    }


    // kontrollerar om användaren saknar en JWT-token
    // om cookien saknas returneras 401 Unauthorized
    private ResponseEntity<String> unauthorizedIfMissing(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Du måste logga in för att komma åt resursen.");
        }

        // ingen felrespons behövs när cookien finns
        return null;
    }

}