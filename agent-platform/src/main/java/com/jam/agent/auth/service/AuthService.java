package com.jam.agent.auth.service;

import com.jam.agent.auth.persistence.repository.UserRepository;
import com.jam.agent.auth.security.AuthenticatedUser;
import com.jam.agent.auth.security.SessionUserAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_]{4,30}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String username, String password) {
        validateCredentials(username, password);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException();
        }
        try {
            userRepository.insert(username, passwordEncoder.encode(password));
        } catch (DuplicateKeyException exception) {
            throw new UsernameAlreadyExistsException();
        }
    }

    public AuthenticatedUser login(String username, String password, HttpServletRequest request) {
        if (username == null || password == null) {
            throw new InvalidCredentialsException();
        }
        Optional<UserRepository.UserRecord> record = userRepository.findByUsername(username);
        if (record.isEmpty() || !record.get().enabled()
                || !passwordEncoder.matches(password, record.get().passwordHash())) {
            throw new InvalidCredentialsException();
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        } else {
            // The custom login endpoint must rotate the ID itself to prevent session fixation.
            request.changeSessionId();
        }
        session.setAttribute(SessionUserAuthenticationFilter.USER_ID_ATTRIBUTE, record.get().id());
        session.setAttribute(SessionUserAuthenticationFilter.USERNAME_ATTRIBUTE, record.get().username());
        session.setAttribute(SessionUserAuthenticationFilter.IS_ADMIN_ATTRIBUTE, record.get().admin());
        return new AuthenticatedUser(record.get().id(), record.get().username(), record.get().admin());
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            // Keep the CSRF-capable Session alive, but remove every attribute that can restore login.
            // Invalidating here can make Spring Session try to save an already invalidated wrapper.
            session.removeAttribute(SessionUserAuthenticationFilter.USER_ID_ATTRIBUTE);
            session.removeAttribute(SessionUserAuthenticationFilter.USERNAME_ATTRIBUTE);
            session.removeAttribute(SessionUserAuthenticationFilter.IS_ADMIN_ATTRIBUTE);
            request.changeSessionId();
        }
        SecurityContextHolder.clearContext();
    }

    private void validateCredentials(String username, String password) {
        if (username == null || !username.matches(USERNAME_PATTERN)) {
            throw new IllegalArgumentException("用户名需为 4-30 位字母、数字或下划线。");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("密码长度需为 8-72 位。");
        }
    }

    public static class UsernameAlreadyExistsException extends RuntimeException {
    }

    public static class InvalidCredentialsException extends RuntimeException {
    }
}
