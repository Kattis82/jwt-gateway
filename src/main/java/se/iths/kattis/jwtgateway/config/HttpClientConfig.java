package se.iths.kattis.jwtgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

// konfigurerar HTTP-klienten som gatewayen använder
@Configuration
public class HttpClientConfig {

    // skapar en HTTP-klient med en maximal anslutningstid på fem sekunder,
    // dvs hur länge klienten väntar på att anslutningen ska upprättas
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }
}
