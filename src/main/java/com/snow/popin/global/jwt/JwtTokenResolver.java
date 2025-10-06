package com.snow.popin.global.jwt;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenResolver {

    public static final String COOKIE_NAME = "jwtToken";
    public static final String BEARER_PREFIX = "Bearer ";

    // 헤더(우선) → 쿠키(대체) 순서
    public String resolve(HttpServletRequest req){

        String bearer = req.getHeader("Authorization");

        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)){
            return bearer.substring(BEARER_PREFIX.length());
        }

        Cookie[] cookies = req.getCookies();
        if (cookies != null){
            for (Cookie c : cookies){
                if (COOKIE_NAME.equals(c.getName())){
                    return c.getValue();
                }
            }
        }

        return null;

    }

}
