CREATE TABLE `study_plan` (
    `id`                   BIGINT       NOT NULL COMMENT '主键ID',
    `user_id`              BIGINT       NOT NULL COMMENT '用户ID',
    `plan_name`            VARCHAR(100) NOT NULL COMMENT '计划名称',
    `exam_name`            VARCHAR(100) DEFAULT NULL COMMENT '考试名称',
    `target_exam_date`     DATE         DEFAULT NULL COMMENT '目标考试日期',
    `daily_target_minutes` INT          DEFAULT NULL COMMENT '每日目标学习分钟数',
    `weekly_target_days`   INT          DEFAULT NULL COMMENT '每周目标学习天数',
    `stage`                VARCHAR(50)  DEFAULT NULL COMMENT '当前阶段',
    `is_current`           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否为当前默认计划: 0-否, 1-是',
    `status`               TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_current` (`user_id`, `is_current`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习计划表';
