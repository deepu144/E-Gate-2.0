package com.kce.egate.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class JWTUtils {
    private static final Logger log = LoggerFactory.getLogger(JWTUtils.class);
    private SecretKey Key;
    @Value("${jwt.expiration}")
    private long EXPIRATION_TIME;
    @Value("${jwt.secret}")
    private String secreteString;
    private final Map<String, Instant> logoutTimes = new ConcurrentHashMap<>();

    @PostConstruct
    public void init(){
        byte[] keyBytes = Base64.getDecoder().decode(secreteString.getBytes(StandardCharsets.UTF_8));
        this.Key = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateToken(HashMap<String, Object> claims, String userName, boolean isAdmin){
        log.debug("[JWTUTILS] generating Admin JWT Token, Username {}", userName);
        JwtBuilder builder = Jwts.builder()
                .claims(claims)
                .subject(userName)
                .issuedAt(new Date(System.currentTimeMillis()))
                .signWith(Key);

        if(isAdmin) {
            builder.expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME));
        } else {
            builder.issuer("717822F110 717822P212");
        }
        return builder.compact();
    }

    public String extractUsername(String token){
        return extractClaims(token, Claims::getSubject);
    }

    public String extractIssuer(String token){
        return extractClaims(token, Claims::getIssuer);
    }

    public Date extractIssuedAt(String token){
        return extractClaims(token, Claims::getIssuedAt);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsTFunction){
        return claimsTFunction.apply(
                Jwts.parser()
                    .verifyWith(Key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
        );
    }

    public void doUserJWTInvalidate(String username) {
        logoutTimes.put(username, Instant.now());
    }

    public boolean isUserTokenValid(String token){
        final String username = extractUsername(token);
        Instant issuedAt = extractIssuedAt(token).toInstant();
        Instant lastLoggedOut = logoutTimes.get(username);
        return lastLoggedOut == null ||  issuedAt.isAfter(lastLoggedOut);
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    public boolean isTokenExpired(String token){
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }
    public String extractValue(String token,String key) {
        return extractClaims(token, i->i.get(key)).toString();
    }
}
