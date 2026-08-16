package com.jam.agent.common.database;

import com.jam.agent.common.database.mapper.SchemaMigrationMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Applies the one compatibility migration needed by databases created before admin support.
 * New installations receive the same column from schema.sql.
 */
@Component
public class DatabaseSchemaMigration implements ApplicationRunner {

    private final SchemaMigrationMapper mapper;

    public DatabaseSchemaMigration(SchemaMigrationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (mapper.countAdminColumn() == 0) {
            mapper.addAdminColumn();
        }
    }
}
