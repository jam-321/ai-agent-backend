package com.jam.agent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionUserAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTRIBUTE = "AUTH_USER_ID";
    public static final String USERNAME_ATTRIBUTE = "AUTH_USERNAME";
    public static final String IS_ADMIN_ATTRIBUTE = "AUTH_IS_ADMIN";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            restoreAuthentication(request);
        }
        filterChain.doFilter(request, response);
    }

    private void restoreAuthentication(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        Object id = session.getAttribute(USER_ID_ATTRIBUTE);
        Object username = session.getAttribute(USERNAME_ATTRIBUTE);
        if (!(id instanceof Number) || !(username instanceof String)) {
            return;
        }

        // Sessions created before is_admin existed have no attribute and remain ordinary users.
        boolean admin = Boolean.TRUE.equals(session.getAttribute(IS_ADMIN_ATTRIBUTE));
        AuthenticatedUser user = new AuthenticatedUser(
                ((Number) id).longValue(),
                (String) username,
                admin);
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                user,
                null,
                authorities(admin));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private List<GrantedAuthority> authorities(boolean admin) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (admin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return authorities;
    }
}
