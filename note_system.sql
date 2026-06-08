CREATE DATABASE IF NOT EXISTS `note_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `note_system`;

CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
  `email` VARCHAR(100) NOT NULL COMMENT '注册邮箱',
  `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER普通用户, ADMIN管理员',
  `nickname` VARCHAR(50) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `signature` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

CREATE TABLE IF NOT EXISTS `categories` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `parent_id` BIGINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_category_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记分类表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记核心表';

CREATE TABLE IF NOT EXISTS `tags` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user_id` (`user_id`),
  UNIQUE KEY `uk_user_tag_name` (`user_id`, `name`),
  CONSTRAINT `fk_tag_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

CREATE TABLE IF NOT EXISTS `note_tags` (
  `note_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  PRIMARY KEY (`note_id`, `tag_id`),
  CONSTRAINT `fk_nt_note` FOREIGN KEY (`note_id`) REFERENCES `notes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nt_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记标签关联表';

CREATE TABLE IF NOT EXISTS `search_logs` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `keyword` VARCHAR(100) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_search_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户搜索记录表';

-- 演示账号，密码均为 password
INSERT INTO `users` (`id`, `username`, `email`, `password`, `role`, `nickname`, `signature`) VALUES
(1, 'admin', 'admin@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiD6PZBttn6L60U3IrIr5CJlOGWzQwG', 'ADMIN', '系统管理员', '负责查看系统整体运行情况'),
(2, 'student', 'student@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiD6PZBttn6L60U3IrIr5CJlOGWzQwG', 'USER', '学习用户', '每天整理一点点')
ON DUPLICATE KEY UPDATE email = VALUES(email), role = VALUES(role), nickname = VALUES(nickname), signature = VALUES(signature);

INSERT INTO `categories` (`id`, `user_id`, `name`, `parent_id`) VALUES
(1, 2, 'Java后端', 0),
(2, 2, 'Vue前端', 0),
(3, 2, '数据库', 0),
(4, 2, '课程复习', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `tags` (`id`, `user_id`, `name`) VALUES
(1, 2, 'SpringBoot'),
(2, 2, 'Vue3'),
(3, 2, 'MySQL'),
(4, 2, 'JWT'),
(5, 2, '复习')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `notes` (`id`, `user_id`, `category_id`, `title`, `content`, `content_text`, `is_starred`, `status`, `last_accessed_at`, `created_at`, `updated_at`) VALUES
(1, 2, 1, 'Spring Boot REST接口设计', '<p>控制器负责接收请求，Service处理业务逻辑。</p>', '控制器负责接收请求，Service处理业务逻辑。', 1, 1, NOW(), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 2, 1, 'JWT登录认证流程', '<p>登录成功后生成Token，前端放入Authorization请求头。</p>', '登录成功后生成Token，前端放入Authorization请求头。', 1, 1, NOW(), DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(3, 2, 2, 'Vue路由守卫', '<p>未登录用户访问业务页面时跳转登录页。</p>', '未登录用户访问业务页面时跳转登录页。', 0, 1, NOW(), DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(4, 2, 2, 'Element Plus表单校验', '<p>使用rules配置用户名、邮箱和密码校验。</p>', '使用rules配置用户名、邮箱和密码校验。', 0, 1, NOW(), DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(5, 2, 3, 'MySQL外键关系', '<p>用户、分类、笔记和标签之间通过外键关联。</p>', '用户、分类、笔记和标签之间通过外键关联。', 0, 1, NOW(), DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(6, 2, 3, '多表关联查询', '<p>笔记列表支持分类、标签、星标、日期组合筛选。</p>', '笔记列表支持分类、标签、星标、日期组合筛选。', 1, 1, NOW(), DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(7, 2, 4, '全栈项目答辩要点', '<p>演示登录、CRUD、分页、角色、统计看板。</p>', '演示登录、CRUD、分页、角色、统计看板。', 1, 1, NOW(), DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(8, 2, 4, '部署环境变量清单', '<p>DB_HOST、DB_PORT、DB_NAME、DB_USER、DB_PASSWORD、JWT_SECRET。</p>', 'DB_HOST、DB_PORT、DB_NAME、DB_USER、DB_PASSWORD、JWT_SECRET。', 0, 1, NOW(), DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(9, 2, 1, '统一异常处理', '<p>使用RestControllerAdvice返回统一JSON错误。</p>', '使用RestControllerAdvice返回统一JSON错误。', 0, 1, NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(10, 2, 2, 'ECharts统计看板', '<p>展示分类占比、创作趋势、高频搜索词。</p>', '展示分类占比、创作趋势、高频搜索词。', 0, 1, NOW(), NOW(), NOW()),
(11, 2, 4, '回收站测试笔记', '<p>这是一条用于演示恢复和永久删除的笔记。</p>', '这是一条用于演示恢复和永久删除的笔记。', 0, 0, NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(12, 2, 2, '自动保存交互', '<p>编辑器内容变化后3秒自动保存。</p>', '编辑器内容变化后3秒自动保存。', 0, 1, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), content_text = VALUES(content_text), status = VALUES(status), is_starred = VALUES(is_starred);

INSERT IGNORE INTO `note_tags` (`note_id`, `tag_id`) VALUES
(1, 1), (2, 1), (2, 4), (3, 2), (4, 2), (5, 3), (6, 3), (7, 5), (9, 1), (10, 2), (12, 2);

INSERT INTO `search_logs` (`user_id`, `keyword`, `created_at`) VALUES
(2, 'SpringBoot', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 'Vue3', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 'JWT', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, '分页', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, '统计', NOW());
