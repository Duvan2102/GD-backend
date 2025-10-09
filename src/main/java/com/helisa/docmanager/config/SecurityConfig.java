package com.helisa.docmanager.config;

import com.helisa.docmanager.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Permitir peticiones OPTIONS para CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // Endpoints públicos de autenticación (solo login y registro)
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/validate-2fa").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/send-email-code").permitAll()
                        
                        // Endpoints públicos de recuperación de contraseña
                        .requestMatchers(HttpMethod.POST, "/api/password-reset/request").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/password-reset/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/password-reset/validate-token").permitAll()
                        
                        // Todos los demás endpoints de auth requieren autenticación
                        .requestMatchers("/api/auth/**").authenticated()
                        
                        // Endpoints de administración
                        .requestMatchers("/api/admin/**").authenticated()
                        .requestMatchers("/api/usuarios/pendientes").authenticated()
                        .requestMatchers("/api/usuarios/*/activar").authenticated()
                        .requestMatchers("/api/usuarios/*/rechazar").authenticated()
                        .requestMatchers("/api/usuarios/*/estado-pendiente").authenticated()
                        
                        // Todos los demás endpoints requieren autenticación
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // IMPORTANTE: En producción, reemplazar "*" con los dominios específicos de tu frontend
        // Ejemplo: Arrays.asList("https://tu-dominio.com", "https://app.tu-dominio.com")
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // Permitir todos los métodos HTTP
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Permitir todos los headers para evitar problemas de CORS
        // En producción puedes restringir a headers específicos
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Permitir credenciales (cookies, authorization headers)
        configuration.setAllowCredentials(false);
        
        // Exponer headers necesarios para el cliente
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "Content-Disposition", "X-Correlation-Id"));
        
        // Tiempo de caché para preflight requests (1 hora)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}

