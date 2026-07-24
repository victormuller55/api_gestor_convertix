package br.net.convertix.gestor.config;

import br.net.convertix.gestor.security.JwtAuthenticationFilter;
import br.net.convertix.gestor.security.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Impede registro duplicado no container Servlet dos filtros que já entram
 * via SecurityFilterChain. Isso evita instanciação precoce (antes do EMF)
 * e mascara a causa real de falhas de inicialização.
 */
@Configuration
@Profile("!test")
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterServletRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> disableRateLimitFilterServletRegistration(
            RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
