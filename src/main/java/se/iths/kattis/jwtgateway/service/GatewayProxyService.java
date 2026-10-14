package se.iths.kattis.jwtgateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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


// hanterar vidarebefordran av anrop från gatewayen till mikrotjänsterna
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

    // hämtar adresserna från application.properties och tar emot HTTP-klienten
    // @Value hämtar adresserna från application.properties.
    // HttpClient skickas in från HttpClientConfig
    public GatewayProxyService(
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



    // bygger ett GET-anrop för att hämta information från en mikrotjänst
    // baseUrl är tjänstens adress och path är sökvägen till resursen
    // ex: http://localhost:8082 + /products
    public ResponseEntity<String> forwardGet(
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

    // bygger ett POST-anrop för att skicka data till en mikrotjänst
    // path = endpoint som ska anropas t ex "/auth/login"
    // requestBody = innehåller t ex JSON med inloggningsuppgifter
    // authorization = eventuell JWT-token som ska skickas vidare
    // contentType = t ex application/json
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


    // hämtar produkter från Product Service
    public ResponseEntity<String> getProducts(String authorization) {

        // använder Product Services adress och dess products-endpoint
        return forwardGet(productServiceUrl, "/products", authorization);
    }

    // hämtar en specifik produkt med hjälp av dess ID
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


    // skickar en ny produkt till Product Service
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

    // tar bort en produkt med det angivna ID:t
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


    // skickar en ny order till Order Service
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


    // bygger ett DELETE-anrop för att ta bort en resurs i en mikrotjänst
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

    // tar emot inloggningsuppgifter från controllern och skickar dem till Auth Server
    // vid lyckad inloggning hanteras JWT-token som en HttpOnly-cookie
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


    // loggar ut användaren genom att instruera webbläsaren att radera JWT-cookien
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

    // metod som skickar anropet (färdigbyggt HTTP-anrop) vidare och
    // returnerar mikrotjänstens svar
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
