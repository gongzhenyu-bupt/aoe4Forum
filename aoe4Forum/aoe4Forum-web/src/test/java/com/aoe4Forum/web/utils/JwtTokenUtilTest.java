package com.aoe4Forum.utils;

import io.jsonwebtoken.Claims;
import org.junit.Test;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtilTest {

    @Test
    public void generateTokenTest(){
        String str = "gzy122";
        String token = JwtTokenUtil.generateToken(str);
        System.out.println(token);
    }
    @Test
    public void parseTokenTest(){
        String str = "gzy122";
        String token = JwtTokenUtil.generateToken(str);
        System.out.println(token);
        Claims claims = JwtTokenUtil.parseToken(token);
        System.out.println(claims.getSubject());
        System.out.println(claims.getExpiration());

    }
}
