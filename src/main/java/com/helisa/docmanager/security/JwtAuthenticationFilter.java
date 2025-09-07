package com.helisa.docmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

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
                username = jwtService.extractUsername(jwt);
                System.out.println("JWT Filter - Username extraído: " + username);
            } catch (Exception e) {
                System.out.println("JWT Filter - Error al extraer username: " + e.getMessage());
                // Token inválido o expirado: continuamos sin autenticar
            }
        } else {
            System.out.println("JWT Filter - No se encontró header Authorization válido");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Verificar si el token está revocado
            try {
                String jti = jwtService.extractJti(jwt);
                if (jti != null && revokedTokenRepository.existsByJti(jti)) {
                    System.out.println("JWT Filter - Token revocado: " + jti);
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                System.out.println("JWT Filter - Error al verificar token revocado: " + e.getMessage());
            }

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("JWT Filter - Usuario autenticado: " + username);
                } else {
                    System.out.println("JWT Filter - Token inválido para usuario: " + username);
                }
            } catch (Exception e) {
                System.out.println("JWT Filter - Error al cargar usuario: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
