package se.iths.kattis.jwtgateway.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
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
    @PostMapping(
            value = "/auth/login",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> login(
            @RequestBody String requestBody,
            HttpServletRequest request) {

        // hämtar eventuell Authorization-header från det inkommande anropet
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // skickar inloggningsuppgifterna vidare till Auth Server
        return proxyService.forwardPost(
                "/auth/login",
                requestBody,
                authorization,
                request.getContentType()
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


    // tar emot frontends begäran om att hämta alla produkter
    @GetMapping("/products")
    public ResponseEntity<String> getProducts(
            HttpServletRequest request) {

        // hämtar eventuell JWT-token från anropet
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // skickar anropet vidare till Product Service
        return proxyService.getProducts(authorization);
    }

    // tar emot en begäran om att hämta en viss produkt
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/products/{id}")
    public ResponseEntity<String> getProductById(
            @PathVariable String id,
            HttpServletRequest request) {

        // hämtar eventuell JWT-token från anropet
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // skickar vidare produktens ID och eventuell JWT-token
        return proxyService.getProductById(id, authorization);
    }


    // tar emot en ny produkt från frontend och skickar den till Product Service
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(
            value = "/products",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> createProduct(
            @RequestBody String requestBody,
            HttpServletRequest request) {

        // hämtar adminanvändarens JWT-token
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // skickar produktdata och token vidare till Product Service
        return proxyService.createProduct(
                requestBody,
                authorization,
                request.getContentType()
        );
    }

    // tar emot en begäran om att ta bort en produkt
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/products/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable String id,
            HttpServletRequest request) {

        // hämtar adminanvändarens JWT-token
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // skickar vidare produktens ID och token
        return proxyService.deleteProduct(id, authorization);
    }


    // tar emot en ny order från frontend
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(
            value = "/orders",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> createOrder(
            @RequestBody String requestBody,
            HttpServletRequest request) {

        // hämtar JWT-token som Order Service behöver för att identifiera kunden
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // skickar orderns JSON-data och headers vidare till Order Service
        return proxyService.createOrder(
                requestBody,
                authorization,
                request.getContentType()
        );
    }

}