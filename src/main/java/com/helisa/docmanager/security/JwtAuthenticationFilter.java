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
import com.helisa.docmanager.repository.TokenRepository;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Saltar el filtro para peticiones OPTIONS (preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                // Verificar si el token está expirado antes de procesarlo
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
            // Verificar si el token está revocado
            try {
                String jti = jwtService.extractJti(jwt);
                if (jti != null && tokenRepository.existsRevokedJwtByJti(jti)) {
                    log.warn("Intento de uso de token revocado - JTI: {}", jti);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token revocado\",\"message\":\"Su sesión ha sido revocada. Por favor, inicie sesión nuevamente.\"}");
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
}
