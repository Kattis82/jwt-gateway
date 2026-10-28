package se.iths.kattis.jwtgateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import se.iths.kattis.jwtgateway.dto.AuthServerResponse;
import se.iths.kattis.jwtgateway.dto.LoginResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


/**
 * Vidarebefordrar anrop från Gateway till mikrotjänsterna (Auth Server, Product Service
 * och Order Service) med Javas {@link HttpClient}. Hanterar också inloggningscookien:
 * JWT från Auth Server sparas i en HttpOnly-cookie och skickas aldrig till frontend i svaret.
 */
@Service
public class GatewayProxyService {

    // adressen till Auth Server, till exempel http://localhost:8080
    private final String authServerUrl;
    // adressen till Product Service, till exempel http://localhost:8082
    private final String productServiceUrl;
    // adressen till Order Service, till exempel http://localhost:8086
    private final String orderServiceUrl;
    // HTTP-klienten som används för att skicka anrop till mikrotjänsterna
    private final HttpClient httpClient;

    private final boolean cookieSecure;
    private final String cookieSameSite;

    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * * Skapar tjänsten med adresserna till mikrotjänsterna och cookie-inställningarna.
     * *
     *
     * @param authServerUrl     adress till Auth Server
     * @param productServiceUrl adress till Product Service
     * @param orderServiceUrl   adress till Order Service
     * @param httpClient        HTTP-klienten som används för anropen
     * @param cookieSecure      om cookien bara får skickas över HTTPS
     * @param cookieSameSite    cookiens SameSite-inställning
     */
    public GatewayProxyService(
            // hämtar adresserna från application.properties och tar emot HTTP-klienten
            // @Value hämtar adresserna från application.properties.
            // HttpClient skickas in från HttpClientConfig
            @Value("${app.auth-server-url}") String authServerUrl,
            @Value("${app.product-service-url}") String productServiceUrl,
            @Value("${app.order-service-url}") String orderServiceUrl,
            HttpClient httpClient,
            // hämtar inställningen för om cookien endast får skickas över HTTPS
            // false används som standard vid lokal utveckling
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            // hämtar SameSite-inställningen för cookien
            // Lax används som standard vid lokal utveckling
            @Value("${app.cookie.same-site:Lax}") String cookieSameSite) {
        this.authServerUrl = authServerUrl;
        this.productServiceUrl = productServiceUrl;
        this.orderServiceUrl = orderServiceUrl;
        this.httpClient = httpClient;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }


    /**
     * Skickar ett GET-anrop till en mikrotjänst.
     *
     * @param baseUrl       tjänstens adress
     * @param path          sökväg till resursen
     * @param authorization värdet för Authorization-headern, eller null
     * @return tjänstens svar
     */
    public ResponseEntity<String> forwardGet(
            // ex: http://localhost:8082 + /products
            String baseUrl,
            String path,
            String authorization) {

        // skapar ett GET-anrop till den angivna adressen
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .GET();

        // skickar anropet vidare till den gemensamma metoden
        // där skickas eventuell JWT-token med och svaret tas emot
        return send(builder, authorization);
    }

    /**
     * Hämtar den inloggade användaren från Auth Server ({@code /auth/me}).
     *
     * @param authorization Bearer-token
     * @return användarnamn och roller, eller felsvar från Auth Server
     */
    // authorization innehåller JWT-tokenen från gatewayens cookie,
    // omgjord till formatet: Bearer <token>
    public ResponseEntity<String> getCurrentUser(String authorization) {

        // skapar ett GET-anrop till Auth Servers /auth/me
        // Auth Server validerar token innan användarinformationen returneras
        return forwardGet(authServerUrl, "/auth/me", authorization);

    }


    /**
     * Skickar ett POST-anrop till Auth Server.
     *
     * @param path          sökväg, t.ex. {@code /users/register}
     * @param requestBody   bodyn som skickas
     * @param authorization värdet för Authorization-headern, eller null
     * @param contentType   Content-Type, eller null
     * @return Auth Servers svar
     */
    public ResponseEntity<String> forwardPost(
            String path,
            String requestBody,
            String authorization,
            String contentType) {

        // skapar ett POST-anrop och lägger till informationen som ska skickas
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(authServerUrl + path))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        // anger vilken typ av data vi skickar, vanligtvis JSON
        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        // skickar anropet vidare och returnerar mikrotjänstens svar
        return send(builder, authorization);
    }


    /**
     * Hämtar alla produkter från Product Service.
     *
     * @param authorization värdet för Authorization-headern, eller null
     * @return Product Services svar
     */
    public ResponseEntity<String> getProducts(String authorization) {

        // använder Product Services adress och dess products-endpoint
        return forwardGet(productServiceUrl, "/products", authorization);
    }

    /**
     * Hämtar en produkt från Product Service.
     *
     * @param id            produktens id
     * @param authorization Bearer-token
     * @return Product Services svar
     */
    public ResponseEntity<String> getProductById(
            String id,
            String authorization) {

        // lägger till produktens ID i adressen
        return forwardGet(
                productServiceUrl,
                "/products/" + id,
                authorization
        );
    }

    /**
     * Skapar en produkt i Product Service.
     *
     * @param requestBody   produktuppgifter som JSON
     * @param authorization Bearer-token
     * @param contentType   Content-Type, eller null
     * @return Product Services svar
     */
    public ResponseEntity<String> createProduct(
            String requestBody,
            String authorization,
            String contentType) {

        // skapar ett POST-anrop till Product Services products-endpoint
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(productServiceUrl + "/products"))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        // anger vilken typ av data som skickas, t ex JSON
        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        // skickar anropet vidare med eventuell JWT-token
        return send(builder, authorization);
    }

    /**
     * Tar bort en produkt i Product Service.
     *
     * @param id            produktens id
     * @param authorization Bearer-token
     * @return Product Services svar
     */
    public ResponseEntity<String> deleteProduct(
            String id,
            String authorization) {

        // använder den gemensamma metoden för DELETE-anrop
        return forwardDelete(
                productServiceUrl,
                "/products/" + id,
                authorization
        );
    }


    /**
     * Skapar en order i Order Service utan betalning.
     *
     * @param requestBody   orderuppgifter som JSON
     * @param authorization Bearer-token
     * @param contentType   Content-Type, eller null
     * @return Order Services svar
     */
    public ResponseEntity<String> createOrder(
            String requestBody,
            String authorization,
            String contentType) {


        // använder Order Services adress och dess orders-endpoint
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(orderServiceUrl + "/orders"))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        // skickar med typen på informationen, t ex JSON
        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        // skickar ordern vidare och returnerar svaret från Order Service
        return send(builder, authorization);
    }


    /**
     * Hämtar den inloggade kundens ordrar från Order Service.
     *
     * @param authorization Bearer-token
     * @return Order Services svar
     */
    public ResponseEntity<String> getCustomerOrders(String authorization) {

        // anropar Order Services GET-endpoint för kundens ordrar
        return forwardGet(orderServiceUrl, "/orders", authorization);
    }


    /**
     * Skickar checkout-anropet till Order Service, som skapar ordern och Stripe Checkout-sessionen.
     * Content-Type sätts alltid till {@code application/json}.
     *
     * @param requestBody   orderuppgifter som JSON
     * @param authorization Bearer-token
     * @param contentType   används inte, Content-Type sätts alltid till JSON
     * @return Order Services svar
     */
    public ResponseEntity<String> createCheckoutOrder(
            String requestBody,
            String authorization,
            String contentType) {

        // skapar ett POST-anrop till Order Services checkout-endpoint
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(
                                URI.create(orderServiceUrl + "/orders/checkout"))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody));


        builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);


        // skickar med JWT-token och returnerar Order Services svar
        return send(builder, authorization);
    }


    /**
     * Skickar ett DELETE-anrop till en mikrotjänst
     *
     * @param baseUrl       tjänstens adress
     * @param path          sökväg till resursen
     * @param authorization värdet för Authorization-headern, eller null
     * @return tjänstens svar
     */
    public ResponseEntity<String> forwardDelete(
            String baseUrl,
            String path,
            String authorization) {

        // skapar ett DELETE-anrop till den angivna adressen
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .DELETE();

        // skickar anropet och returnerar svaret från mikrotjänsten
        return send(builder, authorization);
    }


    // hanterar svaret från Auth Server efter en lyckad inloggning
    // skapar en HttpOnly-cookie med JWT-token och returnerar övrig användarinformation
    // till frontend utan att skicka med själva token i response body
    private ResponseEntity<?> createLoginResponse(
            String responseBody
    ) {
        try {
            // omvandlar Auth Servers JSON-svar till ett Java-objekt
            AuthServerResponse authResponse =
                    objectMapper.readValue(responseBody, AuthServerResponse.class);
            // skapar en cookie som innehåller JWT-token
            ResponseCookie cookie = ResponseCookie.from(
                            "access_token",
                            authResponse.accessToken()
                    )
                    .httpOnly(true) // JavaScript kan inte läsa cookien
                    .secure(cookieSecure) // styr om cookien endast skickas över HTTPS
                    .sameSite(cookieSameSite) // styr när webbläsaren får skicka cookien
                    .path("/") // Cookien gäller för hela applikationen
                    .maxAge(Duration.ofSeconds(authResponse.expiresIn())) // Cookiens livslängd
                    .build();

            // skapar svaret till frontend utan själva JWT-token
            LoginResponse loginResponse = new LoginResponse(
                    authResponse.expiresIn(),
                    authResponse.subject(),
                    authResponse.roles()
            );

            // returnerar användarinformationen och skickar cookien i svarets header
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(loginResponse);
        } catch (JacksonException ex) {
            // returnerar 502 om Auth Servers svar inte kan tolkas
            return ResponseEntity.status(502)
                    .body("Auth Server returnerar ett ogiltigt svar");
        }

    }

    // skickar inloggningsanropet till Auth Server och hanterar svaret
    private ResponseEntity<?> sendLogin(
            HttpRequest.Builder request
    ) {
        try {
            // bygger HTTP-anropet, skickar det och läser svaret som text
            HttpResponse<String> response = httpClient.send(
                    request.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            // om inloggningen misslyckas skickas Auth Servers statuskod och svar vidare
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ResponseEntity
                        .status(response.statusCode())
                        .body(response.body());
            }

            // vid lyckad inloggning skapas en cookie med JWT-token
            return createLoginResponse(response.body());

        } catch (IOException | InterruptedException ex) {
            // återställer avbrottsflaggan om anropet avbröts
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            // returnerar 502 om gatewayen inte kan nå Auth Server
            return ResponseEntity.status(502)
                    .body("Gateway kunde inte nå auth server");
        }

    }


    /**
     * Loggar in via Auth Server. Vid lyckad inloggning sparas JWT i en HttpOnly-cookie
     * ({@code access_token}) och frontend får användarnamn, roller och giltighetstid, men inte token.
     * Vid misslyckad inloggning skickas Auth Servers statuskod och svar vidare.
     *
     * @param requestBody inloggningsuppgifter som JSON
     * @param contentType Content-Type för anropet
     * @return svar med cookie vid lyckad inloggning, 502 om Auth Server inte kan nås eller
     * returnerar ett ogiltigt svar
     */
    public ResponseEntity<?> loginWithCookie(
            String requestBody, String contentType
    ) {
        // skapar ett HTTP-anrop till Auth Servers login-endpoint
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create(authServerUrl + "/auth/login")
                )
                // anger vilken typ av data som skickas, t ex JSON
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        contentType
                )
                // skickar inloggningsuppgifterna i request body
                .POST(
                        HttpRequest.BodyPublishers.ofString(requestBody)
                );
        // skickar anropet och hanterar svaret från Auth Server
        return sendLogin(request);

    }


    /**
     * Loggar ut genom att skicka en cookie som direkt raderas i webbläsaren.
     *
     * @return 204 No Content med cookien som ska raderas
     */
    public ResponseEntity<Void> logout() {
        // skapar en tom cookie med samma inställningar som vid inloggning
        ResponseCookie cookie = ResponseCookie
                .from("access_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                // maxAge = 0 gör att webbläsaren raderar cookien direkt
                .maxAge(Duration.ZERO)
                .build();

        // returnerar 204 No Content och skickar instruktionen att radera cookien
        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
                )
                .build();

    }


    /**
     * Skickar ett färdigbyggt anrop till en mikrotjänst och returnerar svaret med samma
     * statuskod och Content-Type. Authorization-headern skickas med om den finns.
     * Token valideras inte här utan av mottagande tjänst.
     *
     * @param builder       det byggda anropet
     * @param authorization värdet för Authorization-headern, eller null
     * @return tjänstens svar, 502 om tjänsten inte kan nås eller 503 om anropet avbröts
     */
    // de olika metoderna för GET, POST, PUT och DELETE kan använda denna
    public ResponseEntity<String> send(
            HttpRequest.Builder builder, String authorization) {


        // om anropet innehåller en JWT-token skickas den vidare
        // mikrotjänsten kan då kontrollera användarens identitet och behörighet,
        // det görs inte här
        if (authorization != null && !authorization.isBlank()) {
            builder.header(HttpHeaders.AUTHORIZATION, authorization);
        }

        try {
            // bygger HTTP-anropet och skickar det till mottagaren
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    // gör att svaret läses som en textsträng
                    HttpResponse.BodyHandlers.ofString()
            );

            // skapar ett svar med samma HTTP-statuskod som mikrotjänsten
            // 200 = OK, 201 = Created, 401 = Unauthorized
            ResponseEntity.BodyBuilder result =
                    ResponseEntity.status(response.statusCode());

            // hämtar Content-Type från mikrotjänstens svar, t ex application/json
            // den skickas med så att mottagaren vet vilken typ av data som returneras
            response.headers()
                    .firstValue(HttpHeaders.CONTENT_TYPE)
                    .ifPresent(value ->
                            result.header(HttpHeaders.CONTENT_TYPE, value));

            // returnerar mikrotjänstens svarskropp tillsammans med statuskoden
            // och Content-Type, om mikrotjänsten skickade med den
            return result.body(response.body());

        } catch (IOException exception) {

            // detta händer om gatewayen t ex inte kan ansluta
            // till mikrotjänsten eller om kommunikationen misslyckas
            //  om gatewayen fick problem med den bakomliggande tjänsten = 502
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Kunde inte nå bakomliggande tjänst");


            // catch-blocken används för kommunikationsproblem, inte för vanliga HTTP-fel
        } catch (InterruptedException exception) {

            // anropet avbröts medan gatewayen väntade på ett svar
            // avbrottsflaggan återställs så att programmet kan
            // uppmärksamma att tråden har avbrutits
            Thread.currentThread().interrupt();

            // tjänsten kan inte hantera anropet just nu = 503
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Anropet avbröts");
        }
    }
}
