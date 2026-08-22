CREATE TABLE IF NOT EXISTS `app_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(30) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `is_admin` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) DEFAULT NULL,
    `source` VARCHAR(32) NOT NULL DEFAULT 'web',
    `agent_key` VARCHAR(32) NOT NULL DEFAULT 'general',
    `model_provider_key` VARCHAR(64) DEFAULT NULL,
    `model_name` VARCHAR(128) DEFAULT NULL,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_user_updated` (`user_id`, `is_deleted`, `updated_at`, `id`),
    CONSTRAINT `fk_conversation_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `model_provider_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT DEFAULT NULL,
    `provider_key` VARCHAR(64) NOT NULL,
    `provider_name` VARCHAR(128) NOT NULL,
    `protocol_type` VARCHAR(32) NOT NULL DEFAULT 'OPENAI_CHAT_COMPLETIONS',
    `base_url` VARCHAR(500) NOT NULL,
    `endpoint_path` VARCHAR(255) DEFAULT NULL,
    `api_key` VARCHAR(1000) NOT NULL,
    `models` JSON DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_provider_key` (`provider_key`),
    KEY `idx_model_provider_user` (`user_id`, `status`, `id`),
    CONSTRAINT `fk_model_provider_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `agent_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `agent_key` VARCHAR(32) NOT NULL,
    `execution_type` VARCHAR(32) NOT NULL DEFAULT 'LOOP',
    `execution_key` VARCHAR(64) DEFAULT NULL,
    `system_prompt` TEXT,
    `enabled_plugins` JSON,
    `enabled_tools` JSON DEFAULT NULL,
    `magic_params` JSON,
    `image_history_mode` VARCHAR(32) NOT NULL DEFAULT 'SUMMARY_TOOL',
    `model_provider_key` VARCHAR(64) NOT NULL DEFAULT 'deepseek',
    `model_name` VARCHAR(128) NOT NULL DEFAULT 'deepseek-v4-flash',
    `model_temperature` DECIMAL(4,3) NOT NULL DEFAULT 0.700,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_config_key` (`agent_key`),
    CONSTRAINT `fk_agent_config_model_provider`
        FOREIGN KEY (`model_provider_key`) REFERENCES `model_provider_config` (`provider_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `conversation_turn` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL,
    `turn_id` INT NOT NULL,
    `type` VARCHAR(16) NOT NULL,
    `content` LONGTEXT NOT NULL,
    `is_hidden` TINYINT NOT NULL DEFAULT 0,
    `error_message` VARCHAR(1000) DEFAULT NULL,
    `trace_id` VARCHAR(64) NOT NULL,
    `agent_key` VARCHAR(32) DEFAULT NULL,
    `model_provider_key` VARCHAR(64) DEFAULT NULL,
    `model_name` VARCHAR(128) DEFAULT NULL,
    `protocol_type` VARCHAR(32) DEFAULT NULL,
    `feedback_type` TINYINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_turn_type` (`conversation_id`, `turn_id`, `type`),
    KEY `idx_conversation_turn_order` (`conversation_id`, `turn_id`, `id`),
    KEY `idx_conversation_turn_trace` (`trace_id`),
    CONSTRAINT `fk_conversation_turn_conversation`
        FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `conversation_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL,
    `turn_id` INT NOT NULL,
    `trace_id` VARCHAR(64) NOT NULL,
    `attempt_no` SMALLINT NOT NULL DEFAULT 1,
    `round_no` INT DEFAULT NULL,
    `call_index` INT DEFAULT NULL,
    `node_id` VARCHAR(64) NOT NULL,
    `node_name` VARCHAR(128) NOT NULL,
    `aggr_key` VARCHAR(128) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `status` VARCHAR(16) NOT NULL,
    `content` LONGTEXT,
    `attachment_id` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_node_turn_order` (`conversation_id`, `turn_id`, `id`),
    KEY `idx_node_turn_aggr` (`conversation_id`, `turn_id`, `aggr_key`, `id`),
    KEY `idx_node_trace_round` (`trace_id`, `attempt_no`, `round_no`, `id`),
    CONSTRAINT `fk_conversation_node_conversation`
        FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `media_asset` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `owner_id` BIGINT NOT NULL,
    `asset_type` VARCHAR(16) NOT NULL DEFAULT 'IMAGE',
    `storage_key` VARCHAR(500) NOT NULL,
    `original_filename` VARCHAR(255) DEFAULT NULL,
    `content_type` VARCHAR(128) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `sha256` VARCHAR(64) NOT NULL,
    `width` INT DEFAULT NULL,
    `height` INT DEFAULT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `summary` TEXT DEFAULT NULL,
    `summary_model` VARCHAR(128) DEFAULT NULL,
    `summary_created_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_media_asset_owner` (`owner_id`, `status`, `id`),
    UNIQUE KEY `uk_media_asset_sha256` (`owner_id`, `sha256`),
    CONSTRAINT `fk_media_asset_owner` FOREIGN KEY (`owner_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `conversation_turn_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL,
    `turn_id` INT NOT NULL,
    `asset_id` BIGINT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `detail` VARCHAR(16) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_turn_attachment` (`conversation_id`, `turn_id`, `asset_id`),
    KEY `idx_turn_attachment_asset` (`asset_id`),
    CONSTRAINT `fk_turn_attachment_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`),
    CONSTRAINT `fk_turn_attachment_asset` FOREIGN KEY (`asset_id`) REFERENCES `media_asset` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admin_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `username` VARCHAR(30) NOT NULL,
    `action` VARCHAR(64) NOT NULL,
    `target_type` VARCHAR(64) NOT NULL,
    `target_id` VARCHAR(128) DEFAULT NULL,
    `request_method` VARCHAR(16) DEFAULT NULL,
    `request_uri` VARCHAR(500) DEFAULT NULL,
    `result` VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    `detail` TEXT,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_admin_audit_created` (`created_at`, `id`),
    KEY `idx_admin_audit_user` (`user_id`, `created_at`),
    CONSTRAINT `fk_admin_audit_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
