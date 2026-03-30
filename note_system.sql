
USE `note_system`;

-- 2. 用户表 (包含 email 用于密码找回)
CREATE TABLE IF NOT EXISTS `users` (
                                       `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
                                       `email` VARCHAR(100) NOT NULL COMMENT '注册邮箱',
                                       `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
                                       `nickname` VARCHAR(50) DEFAULT NULL,
                                       `avatar` VARCHAR(255) DEFAULT NULL,
                                       `signature` VARCHAR(255) DEFAULT NULL,
                                       `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户信息表';

-- 3. 分类表
CREATE TABLE IF NOT EXISTS `categories` (
                                            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            `user_id` BIGINT NOT NULL,
                                            `name` VARCHAR(50) NOT NULL,
                                            `parent_id` BIGINT DEFAULT 0,
                                            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                            `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                            KEY `idx_user_id` (`user_id`),
                                            CONSTRAINT `fk_category_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='笔记分类表';

-- 4. 笔记核心表 (包含 last_accessed_at 用于最近访问)
CREATE TABLE IF NOT EXISTS `notes` (
                                       `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       `user_id` BIGINT NOT NULL,
                                       `category_id` BIGINT DEFAULT NULL,
                                       `title` VARCHAR(150) NOT NULL,
                                       `content` LONGTEXT,
                                       `content_text` LONGTEXT,
                                       `is_starred` TINYINT(1) DEFAULT 0,
                                       `status` TINYINT(1) DEFAULT 1 COMMENT '1正常, 0回收站',
                                       `last_accessed_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间',
                                       `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       `deleted_at` DATETIME DEFAULT NULL,
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_category_id` (`category_id`),
                                       CONSTRAINT `fk_note_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
                                       CONSTRAINT `fk_note_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='笔记核心表';

-- 5. 标签表
CREATE TABLE IF NOT EXISTS `tags` (
                                      `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `user_id` BIGINT NOT NULL,
                                      `name` VARCHAR(50) NOT NULL,
                                      `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                      KEY `idx_user_id` (`user_id`),
                                      CONSTRAINT `fk_tag_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='标签表';

-- 6. 笔记-标签关联表
CREATE TABLE IF NOT EXISTS `note_tags` (
                                           `note_id` BIGINT NOT NULL,
                                           `tag_id` BIGINT NOT NULL,
                                           PRIMARY KEY (`note_id`, `tag_id`),
                                           CONSTRAINT `fk_nt_note` FOREIGN KEY (`note_id`) REFERENCES `notes` (`id`) ON DELETE CASCADE,
                                           CONSTRAINT `fk_nt_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. 搜索日志表 (新增：用于行为统计)
CREATE TABLE IF NOT EXISTS `search_logs` (
                                             `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             `user_id` BIGINT NOT NULL,
                                             `keyword` VARCHAR(100) NOT NULL,
                                             `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                             KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='用户搜索记录表';