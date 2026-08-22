package com.jam.agent.auth.persistence.repository;

import com.jam.agent.auth.persistence.entity.AppUserEntity;
import com.jam.agent.auth.persistence.mapper.AppUserMapper;
import java.util.Optional;
import java.util.List;
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

    public List<UserRecord> findPage(int offset, int size) {
        return mapper.selectAdminPage(offset, size).stream().map(this::toRecord).toList();
    }

    public long countAll() {
        return mapper.countAllUsers();
    }

    public long countAdmins() {
        return mapper.countAdmins();
    }

    public void updateAdmin(long id, boolean admin) {
        AppUserEntity entity = mapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在。");
        }
        entity.setAdmin(admin);
        mapper.updateById(entity);
    }

    public void updateEnabled(long id, boolean enabled) {
        AppUserEntity entity = mapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在。");
        }
        entity.setEnabled(enabled);
        mapper.updateById(entity);
    }

    private UserRecord toRecord(AppUserEntity user) {
        return new UserRecord(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getEnabled()),
                Boolean.TRUE.equals(user.getAdmin()),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public record UserRecord(
            Long id,
            String username,
            String passwordHash,
            boolean enabled,
            boolean admin,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
    }
}
