package com.jam.agent.common.database.mapper;

import org.apache.ibatis.annotations.Param;

public interface SchemaMigrationMapper {

    int countAdminColumn();

    void addAdminColumn();
}
