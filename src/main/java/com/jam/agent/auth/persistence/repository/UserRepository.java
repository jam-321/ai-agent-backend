package com.jam.agent.auth.persistence.repository;

import com.jam.agent.auth.persistence.entity.AppUserEntity;
import com.jam.agent.auth.persistence.mapper.AppUserMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final AppUserMapper mapper;

    public UserRepository(AppUserMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<UserRecord> findByUsername(String username) {
        return Optional.ofNullable(mapper.selectByUsername(username))
                .map(this::toRecord);
    }

    public long insert(String username, String passwordHash) {
        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        mapper.insert(user);

        if (user.getId() == null) {
            throw new IllegalStateException("创建用户失败。");
        }
        return user.getId();
    }

    private UserRecord toRecord(AppUserEntity user) {
        return new UserRecord(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getEnabled()),
                Boolean.TRUE.equals(user.getAdmin()));
    }

    public record UserRecord(
            Long id,
            String username,
            String passwordHash,
            boolean enabled,
            boolean admin) {
    }
}
