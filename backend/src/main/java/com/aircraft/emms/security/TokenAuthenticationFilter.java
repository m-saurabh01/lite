package com.aircraft.emms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenStore tokenStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            TokenStore.TokenEntry entry = tokenStore.validateToken(token);
            if (entry != null) {
                List<SimpleGrantedAuthority> authorities = Arrays.stream(entry.getRoles().split(","))
                        .map(String::trim)
                        .filter(r -> !r.isEmpty())
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();
                var auth = new UsernamePasswordAuthenticationToken(
                        entry.getServiceId(),
                        null,
                        authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
