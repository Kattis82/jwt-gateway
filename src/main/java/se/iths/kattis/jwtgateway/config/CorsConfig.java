package se.iths.kattis.jwtgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    // skapar ett CORS-filter som Spring använder för inkommande HTTP-anrop
    @Bean
    public CorsFilter corsFilter() {

        // samlar inställningarna för vilka frontend-anrop som ska tillåtas
        CorsConfiguration config = new CorsConfiguration();

        // tillåter våra lokala frontend-adresser
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175",
                "https://webshoppen-kjpj.onrender.com"
        ));

        // tillåter de HTTP-metoder som frontend behöver använda
        // OPTIONS behövs bl a för webbläsarens CORS-kontroller
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // tillåter headers som frontend kan behöva skicka
        // JWT skickas nu till gatewayen via en cookie
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type"
        ));

        // tillåter cookies vid CORS-anrop
        // och specifika origins används
        config.setAllowCredentials(true);

        // kopplar CORS-inställningarna till alla sökvägar i gatewayen
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        // returnerar filtret så att Spring kan använda det
        return new CorsFilter(source);
    }

}
