package com.jam.agent.mapper;

import org.apache.ibatis.annotations.Param;

public interface SchemaMigrationMapper {

    int countAdminColumn();

    void addAdminColumn();
}
