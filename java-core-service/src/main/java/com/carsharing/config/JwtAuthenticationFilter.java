package com.carsharing.config;

import com.carsharing.controllers.AuthController;
import com.carsharing.entities.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Simple JWT authentication filter for demonstration purposes.
 * In production, use a proper JWT library like jjwt.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // In production, this would be injected or retrieved from a proper token service
    // For now, we'll use a simple approach
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Skip authentication for public endpoints
        String path = request.getServletPath();
        if (path.startsWith("/api/auth/") || 
            path.startsWith("/api/legacy/") ||
            path.equals("/api/users/register") || 
            path.equals("/api/users/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        // Validate token against in-memory store
        if (token != null && !token.trim().isEmpty() && AuthController.getInstance().isValidToken(token)) {
            UUID userId = AuthController.getInstance().getUserIdFromToken(token);
            if (userId != null) {
                User user = new User();
                user.setId(userId);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        Collections.emptyList()
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean requiresAuthentication(String path) {
        return path.startsWith("/api/users/") && 
               !path.equals("/api/users/register") && 
               !path.equals("/api/users/login") ||
               path.startsWith("/api/rides/") ||
               path.startsWith("/api/payments/") ||
               path.startsWith("/api/vehicles/") ||
               path.startsWith("/api/telemetry/");
    }
}