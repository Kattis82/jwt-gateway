package se.iths.kattis.jwtgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Konfigurerar HTTP-klienten som Gateway använder mot mikrotjänsterna.
 */
@Configuration
public class HttpClientConfig {


    /**
     * Skapar en {@link HttpClient} med en anslutningstid på högst fem sekunder.
     *
     * @return konfigurerad HttpClient
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                // hur länge klienten väntar på att anslutningen ska upprättas
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }
}
