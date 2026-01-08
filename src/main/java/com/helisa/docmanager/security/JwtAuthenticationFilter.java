package com.helisa.docmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.helisa.docmanager.repository.RevokedTokenRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register", 
        "/api/auth/validate-2fa",
        "/api/auth/send-email-code",
        "/api/auth/2fa-status/",
        "/api/auth/logout",
        "/api/password-reset/request",
        "/api/password-reset/confirm",
        "/api/password-reset/validate-token"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String requestMethod = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
            log.debug("Saltando filtro JWT para petición OPTIONS: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        if (isPublicEndpoint(requestPath, requestMethod)) {
            log.debug("Saltando filtro JWT para endpoint público: {} {}", requestMethod, requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                if (jwtService.isTokenExpired(jwt)) {
                    log.warn("Token expirado recibido");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token expirado\",\"message\":\"Su sesión ha expirado. Por favor, inicie sesión nuevamente.\"}");
                    return;
                }
                
                username = jwtService.extractUsername(jwt);
                log.debug("Usuario extraído del token: {}", username);
            } catch (Exception e) {
                log.warn("Error al procesar token JWT: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token inválido\",\"message\":\"Token de autenticación inválido. Por favor, inicie sesión nuevamente.\"}");
                return;
            }
        } else {
            log.debug("No se encontró header Authorization en la petición a: {}", request.getRequestURI());
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                String jti = jwtService.extractJti(jwt);
                if (revokedTokenRepository.existsByJti(jti)) {
                    log.warn("Intento de uso de token revocado - JTI: {}", jti);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token revocado\",\"message\":\"Su sesión ha sido cerrada. Por favor, inicie sesión nuevamente.\"}");
                    return;
                }
            } catch (Exception e) {
                log.error("Error al verificar token revocado: {}", e.getMessage());
            }

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuario autenticado exitosamente: {}", username);
                } else {
                    log.warn("Token inválido para usuario: {}", username);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token inválido\",\"message\":\"Token de autenticación inválido. Por favor, inicie sesión nuevamente.\"}");
                    return;
                }
            } catch (Exception e) {
                log.error("Error al cargar usuario durante autenticación: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Error de autenticación\",\"message\":\"Error al verificar credenciales. Por favor, inicie sesión nuevamente.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestPath, String requestMethod) {
        for (String publicEndpoint : PUBLIC_ENDPOINTS) {
            if (requestPath.startsWith(publicEndpoint)) {
                if ("/api/auth/login".equals(publicEndpoint) && "POST".equals(requestMethod)) {
                    return true;
                }
                if ("/api/auth/register".equals(publicEndpoint) && "POST".equals(requestMethod)) {
                    return true;
                }
                if ("/api/auth/validate-2fa".equals(publicEndpoint) && "POST".equals(requestMethod)) {
                    return true;
                }
                if ("/api/auth/send-email-code".equals(publicEndpoint) && "POST".equals(requestMethod)) {
                    return true;
                }
                if ("/api/auth/2fa-status/".equals(publicEndpoint) && "GET".equals(requestMethod)) {
                    return true;
                }
                if ("/api/auth/logout".equals(publicEndpoint) && "POST".equals(requestMethod)) {
                    return true;
                }
                if ("/api/password-reset/request".equals(publicEndpoint) && "POST".equals(requestMethod)) {
                    return true;
                }
                if ("/api/password-reset/confirm".equals(publicEndpoint) && "POST".equals(requestMethod)) {
                    return true;
                }
                if ("/api/password-reset/validate-token".equals(publicEndpoint) && "GET".equals(requestMethod)) {
                    return true;
                }
            }
        }
        return false;
    }
}
