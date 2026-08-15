package com.jam.agent.security;

import java.io.Serializable;

public record AuthenticatedUser(Long id, String username) implements Serializable {

    private static final long serialVersionUID = 1L;
}
