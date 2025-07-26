package com.aoe4Forum.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;

import java.util.Date;

@Component
public class JwtTokenUtil {
    @Value("${token.secretKey}")
    private static String secretKey;
    private static final String TEST_SECRET_KEY = "gzy12300";


    public static String generateToken(String username){
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(SignatureAlgorithm.HS256, TEST_SECRET_KEY)
                .compact();
    }

    public static Claims parseToken(String token){
        return Jwts.parser()
                .setSigningKey(TEST_SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }

    public static boolean validateToken(String token){
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        }catch (ExpiredJwtException e){
            return false;
        }
    }
}