package se.iths.kattis.jwtgateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.kattis.jwtgateway.service.GatewayProxyService;


/**
 * Gatewayens publika API för frontend. Alla endpoints nås via {@code /gateway}.
 * JWT läses från HttpOnly-cookien {@code access_token} och skickas vidare till
 * mikrotjänsterna som {@code Authorization: Bearer <token>}.
 * Skyddade endpoints svarar 401 om cookien saknas. Gateway kontrollerar bara att
 * cookien finns, tjänsten som tar emot anropet validerar token.
 */
@RestController
@RequestMapping("/gateway")
public class GatewayController {

    // används för att vidarebefordra anrop till mikrotjänsterna
    private final GatewayProxyService proxyService;

    public GatewayController(GatewayProxyService proxyService) {
        this.proxyService = proxyService;
    }


    /**
     * Loggar in via Auth Server. JWT sparas i en HttpOnly-cookie.
     *
     * @param requestBody inloggningsuppgifterna som JSON
     * @param request     det inkommande anropet, används för Content-Type
     * @return svaret från Auth Server tillsammans med inloggningscookien
     */
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

    /**
     * Registrerar en ny användare via Auth Server. Kräver ingen inloggning.
     *
     * @param requestBody registreringsuppgifterna som JSON
     * @param request     det inkommande anropet
     * @return svaret från Auth Server
     */
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

    /**
     * Loggar ut genom att ta bort inloggningscookien.
     *
     * @return tomt svar
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return proxyService.logout();
    }


    /**
     * Returnerar den inloggade användaren. Frontend anropar den när appen laddas om
     * för att återställa inloggningen.
     *
     * @param accessToken JWT från cookien, kan vara null
     * @return användarnamn och roller från Auth Server, eller 401 om cookien saknas
     */
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


    /**
     * Hämtar alla produkter från Product Service. Kräver ingen inloggning.
     *
     * @return produkterna som JSON
     */
    @GetMapping("/products")
    public ResponseEntity<String> getProducts() {

        // skickar anropet vidare till Product Service utan JWT-token
        return proxyService.getProducts(null);
    }

    /**
     * Hämtar en produkt. Kräver inloggning.
     *
     * @param id          produktens id
     * @param accessToken JWT från cookien, kan vara null
     * @return produkten som JSON, eller 401 om cookien saknas
     */
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


    /**
     * Skapar en produkt i Product Service. Kräver inloggning.
     *
     * @param requestBody produktuppgifter som JSON
     * @param accessToken JWT från cookien, kan vara null
     * @param request     det inkommande anropet, används för Content-Type
     * @return svaret från Product Service, eller 401 om cookien saknas
     */
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


    /**
     * Tar bort en produkt i Product Service. Kräver inloggning.
     *
     * @param id          produktens id
     * @param accessToken JWT från cookien, kan vara null
     * @return svaret från Product Service, eller 401 om cookien saknas
     */
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


    /**
     * Skapar en order i Order Service utan betalning. Kräver inloggning.
     *
     * @param requestBody orderns produkter som JSON
     * @param accessToken JWT från cookien, kan vara null
     * @param request     det inkommande anropet, används för Content-Type
     * @return svaret från Order Service, eller 401 om cookien saknas
     */
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


    /**
     * Hämtar den inloggade kundens orderhistorik från Order Service.
     *
     * @param accessToken JWT från cookien, kan vara null
     * @return kundens ordrar som JSON, eller 401 om cookien saknas
     */
    @GetMapping("/orders")
    public ResponseEntity<String> getCustomerOrders(
            @CookieValue(name = "access_token", required = false)
            String accessToken) {

        // kontrollerar att användaren har en JWT-cookie
        ResponseEntity<String> unauthorized =
                unauthorizedIfMissing(accessToken);

        // returnerar 401 om användaren inte är inloggad
        if (unauthorized != null) {
            return unauthorized;
        }

        // gör om token från cookien till en Bearer-token
        String authorization = getAuthorizationFromCookie(accessToken);

        // hämtar kundens ordrar från Order Service
        return proxyService.getCustomerOrders(authorization);
    }

    /**
     * Skapar en order och startar Stripe Checkout via Order Service. Kräver inloggning.
     *
     * @param requestBody orderns produkter som JSON
     * @param contentType anropets Content-Type, kan vara null
     * @param token       JWT från cookien, kan vara null
     * @return order-id, session-id och betalningslänk, eller 401 om cookien saknas
     */
    @PostMapping("/orders/checkout")
    public ResponseEntity<String> createCheckoutOrder(
            @RequestBody String requestBody,
            @RequestHeader(
                    value = HttpHeaders.CONTENT_TYPE,
                    required = false) String contentType,
            @CookieValue(
                    name = "access_token",
                    required = false) String token) {

        // kontrollera att användaren har en JWT-cookie
        ResponseEntity<String> unauthorized =
                unauthorizedIfMissing(token);

        if (unauthorized != null) {
            return unauthorized;
        }

        // gör om cookiens token till formatet som Order Service förväntar sig
        String authorization = getAuthorizationFromCookie(token);

        // skickar ordern vidare till Order Service för Checkout
        return proxyService.createCheckoutOrder(
                requestBody,
                authorization,
                contentType);
    }


    // hjälpmetoder

    /**
     * Gör om JWT från cookien till en Bearer-token.
     *
     * @param accessToken JWT från cookien
     * @return {@code "Bearer " + token}, eller null om token saknas
     */
    private String getAuthorizationFromCookie(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }

        return "Bearer " + accessToken;
    }


    /**
     * Kontrollerar att en JWT finns.
     *
     * @param accessToken JWT från cookien
     * @return 401 Unauthorized om token saknas, annars null
     */
    private ResponseEntity<String> unauthorizedIfMissing(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Du måste logga in för att komma åt resursen.");
        }

        // ingen felrespons behövs när cookien finns
        return null;
    }

}