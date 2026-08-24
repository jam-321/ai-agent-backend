package com.jam.agent.auth.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jam.agent.auth.persistence.repository.UserRepository;
import com.jam.agent.auth.security.SessionUserAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logoutClearsAuthenticationWithoutInvalidatingRedisSession() {
        AuthService service = new AuthService(
                mock(UserRepository.class),
                mock(PasswordEncoder.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));

        service.logout(request);

        verify(session).removeAttribute(SessionUserAuthenticationFilter.USER_ID_ATTRIBUTE);
        verify(session).removeAttribute(SessionUserAuthenticationFilter.USERNAME_ATTRIBUTE);
        verify(session).removeAttribute(SessionUserAuthenticationFilter.IS_ADMIN_ATTRIBUTE);
        verify(request).changeSessionId();
        verify(session, never()).invalidate();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
