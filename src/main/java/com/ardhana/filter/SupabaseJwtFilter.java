package com.ardhana.filter;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.util.List;

@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private static final String JWKS_URL =
        "https://uiiqatykujuqsnhgvzne.supabase.co/auth/v1/.well-known/jwks.json";
    private static final String ISSUER =
        "https://uiiqatykujuqsnhgvzne.supabase.co/auth/v1";

    private final JwkProvider jwkProvider;

    public SupabaseJwtFilter() throws Exception {
        this.jwkProvider = new UrlJwkProvider(new URL(JWKS_URL));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            DecodedJWT unverified = JWT.decode(token);
            String kid = unverified.getKeyId();

            Jwk jwk = jwkProvider.get(kid);
            ECPublicKey publicKey = (ECPublicKey) jwk.getPublicKey();

            DecodedJWT jwt = JWT.require(Algorithm.ECDSA256(publicKey, null))
                    .withIssuer(ISSUER)
                    .acceptLeeway(10)
                    .build()
                    .verify(token);

            String uid = jwt.getSubject();
            String role = jwt.getClaim("role").asString();
            System.out.println(">>> JWT valid — uid: " + uid + ", role: " + role);

            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    uid, null,
                    List.of(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"))
                );
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            System.out.println(">>> JWT error: " + e.getClass().getName() + " — " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"Invalid or expired token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}