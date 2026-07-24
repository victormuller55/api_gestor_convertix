package br.net.convertix.gestor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${app.cors.exposed-headers}")
    private String exposedHeaders;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Rotas públicas (BioLink publicado + uploads): qualquer origem.
        // Sem isso, o Spring responde 403 "Invalid CORS request" quando o front
        // chama de um domínio/porta fora da lista restrita.
        CorsConfiguration publicCors = new CorsConfiguration();
        publicCors.setAllowedOriginPatterns(List.of("*"));
        publicCors.setAllowedMethods(List.of("GET", "OPTIONS"));
        publicCors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        publicCors.setExposedHeaders(split(exposedHeaders));
        publicCors.setAllowCredentials(false);
        publicCors.setMaxAge(maxAge);
        source.registerCorsConfiguration("/api/v1/biolinks/publico", publicCors);
        source.registerCorsConfiguration("/api/v1/biolinks/publico/**", publicCors);
        source.registerCorsConfiguration("/uploads/**", publicCors);

        // Demais rotas da API: apenas origens configuradas (gestor / localhost).
        CorsConfiguration privateCors = new CorsConfiguration();
        privateCors.setAllowedOrigins(split(allowedOrigins));
        privateCors.setAllowedMethods(split(allowedMethods));
        privateCors.setAllowedHeaders(split(allowedHeaders));
        privateCors.setExposedHeaders(split(exposedHeaders));
        privateCors.setAllowCredentials(false);
        privateCors.setMaxAge(maxAge);
        source.registerCorsConfiguration("/**", privateCors);

        return source;
    }

    private static List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
