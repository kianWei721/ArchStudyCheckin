# ArchStudyCheckin 数据库设计文档

## 1. 数据库基本信息

| 项目 | 说明 |
|------|------|
| 数据库 | MySQL 8.0+ |
| 字符集 | utf8mb4 |
| 排序规则 | utf8mb4_general_ci |
| 存储引擎 | InnoDB |
| 命名规范 | 表名、字段名均使用下划线命名（snake_case） |
| 主键策略 | 雪花算法生成 BIGINT 类型 ID |
| 时间字段 | 统一使用 DATETIME 类型，存储时区 Asia/Shanghai |
| 逻辑删除 | 使用 `deleted` 字段（0=未删除，1=已删除） |
| 公共字段 | 所有表包含 `create_time`、`update_time`、`deleted` |

---

## 2. ER 关系概览

```
user (1) ──── (N) study_plan
user (1) ──── (N) checkin_record
study_plan (1) ──── (N) checkin_record
user (1) ──── (N) study_group (创建关系)
user (N) ──── (N) study_group (通过 group_member 多对多)
```

---

## 3. 表结构设计

### 3.1 用户表 `user`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 用户ID（雪花算法） |
| username | VARCHAR(20) | NOT NULL, UNIQUE | 用户名 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 邮箱 |
| password | VARCHAR(100) | NOT NULL | 密码（BCrypt加密） |
| avatar_url | VARCHAR(255) | NULL | 头像URL（预留） |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `uk_email` UNIQUE (email)
- `uk_username` UNIQUE (username)

---

### 3.2 学习计划表 `study_plan`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 计划ID |
| user_id | BIGINT | NOT NULL | 所属用户ID |
| plan_name | VARCHAR(50) | NOT NULL | 计划名称 |
| subject | VARCHAR(50) | NOT NULL | 科目（如"系统架构设计"） |
| target_days | INT | NOT NULL | 目标天数 |
| start_date | DATE | NOT NULL | 开始日期 |
| status | TINYINT | NOT NULL, DEFAULT 0 | 状态：0=进行中，1=已完成，2=已放弃 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `idx_user_id` (user_id)
- `idx_user_status` (user_id, status)

---

### 3.3 打卡记录表 `checkin_record`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 记录ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| plan_id | BIGINT | NOT NULL | 关联学习计划ID |
| checkin_date | DATE | NOT NULL | 打卡日期（用户本地时区） |
| duration | INT | NOT NULL | 学习时长（分钟） |
| content | VARCHAR(200) | NULL | 学习内容摘要 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `uk_user_plan_date` UNIQUE (user_id, plan_id, checkin_date) — 保证打卡幂等
- `idx_user_date` (user_id, checkin_date) — 查询今日/月度打卡
- `idx_plan_id` (plan_id)

**幂等说明：**
- 唯一索引 `uk_user_plan_date` 保证同一用户同一天同一计划只有一条记录
- 业务层使用 `INSERT ... ON DUPLICATE KEY UPDATE` 或先查后更新的方式实现幂等

---

### 3.4 学习小组表 `study_group`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 小组ID |
| group_name | VARCHAR(30) | NOT NULL | 小组名称 |
| description | VARCHAR(200) | NULL | 小组简介 |
| invite_code | VARCHAR(6) | NOT NULL, UNIQUE | 邀请码（6位大写字母+数字） |
| owner_id | BIGINT | NOT NULL | 组长用户ID |
| member_count | INT | NOT NULL, DEFAULT 1 | 当前成员数 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `uk_invite_code` UNIQUE (invite_code)
- `idx_owner_id` (owner_id)

---

### 3.5 小组成员表 `group_member`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 记录ID |
| group_id | BIGINT | NOT NULL | 小组ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| role | TINYINT | NOT NULL, DEFAULT 0 | 角色：0=普通成员，1=组长 |
| join_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 加入时间 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `uk_group_user` UNIQUE (group_id, user_id) — 防止重复加入
- `idx_user_id` (user_id) — 查询用户所在小组
- `idx_group_id` (group_id) — 查询小组成员列表

---

## 4. DDL 建表语句

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS arch_study_checkin
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE arch_study_checkin;

-- 用户表
CREATE TABLE `user` (
  `id` BIGINT NOT NULL COMMENT '用户ID',
  `username` VARCHAR(20) NOT NULL COMMENT '用户名',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- 学习计划表
CREATE TABLE `study_plan` (
  `id` BIGINT NOT NULL COMMENT '计划ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `plan_name` VARCHAR(50) NOT NULL COMMENT '计划名称',
  `subject` VARCHAR(50) NOT NULL COMMENT '科目',
  `target_days` INT NOT NULL COMMENT '目标天数',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0=进行中,1=已完成,2=已放弃)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学习计划表';

-- 打卡记录表
CREATE TABLE `checkin_record` (
  `id` BIGINT NOT NULL COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `plan_id` BIGINT NOT NULL COMMENT '学习计划ID',
  `checkin_date` DATE NOT NULL COMMENT '打卡日期(用户本地时区)',
  `duration` INT NOT NULL COMMENT '学习时长(分钟)',
  `content` VARCHAR(200) DEFAULT NULL COMMENT '学习内容摘要',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_plan_date` (`user_id`, `plan_id`, `checkin_date`),
  KEY `idx_user_date` (`user_id`, `checkin_date`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='打卡记录表';

-- 学习小组表
CREATE TABLE `study_group` (
  `id` BIGINT NOT NULL COMMENT '小组ID',
  `group_name` VARCHAR(30) NOT NULL COMMENT '小组名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '小组简介',
  `invite_code` VARCHAR(6) NOT NULL COMMENT '邀请码(6位)',
  `owner_id` BIGINT NOT NULL COMMENT '组长用户ID',
  `member_count` INT NOT NULL DEFAULT 1 COMMENT '成员数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学习小组表';

-- 小组成员表
CREATE TABLE `group_member` (
  `id` BIGINT NOT NULL COMMENT '记录ID',
  `group_id` BIGINT NOT NULL COMMENT '小组ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色(0=成员,1=组长)',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='小组成员表';
```

---

## 5. 数据风险说明

| 操作 | 风险等级 | 说明 |
|------|----------|------|
| 用户注册 | 低 | INSERT user，唯一索引防止重复注册 |
| 创建学习计划 | 低 | INSERT study_plan，无唯一约束冲突风险 |
| 打卡（新增/更新） | 中 | 唯一索引保证幂等，重复提交走更新逻辑 |
| 创建小组 | 低 | INSERT study_group + group_member，邀请码唯一校验 |
| 加入小组 | 中 | INSERT group_member + UPDATE member_count，需事务保证一致性 |
| 退出小组 | 中 | 逻辑删除 group_member + UPDATE member_count，需事务 |
| 解散小组 | 高 | 逻辑删除 study_group + 所有 group_member，仅组长可操作 |

---

## 6. 字段映射说明（数据库 → Java）

| 数据库字段（下划线） | Java 字段（驼峰） | 说明 |
|---------------------|-------------------|------|
| user_id | userId | MyBatis-Plus 自动映射 |
| plan_name | planName | |
| checkin_date | checkinDate | |
| target_days | targetDays | |
| start_date | startDate | |
| invite_code | inviteCode | |
| owner_id | ownerId | |
| member_count | memberCount | |
| group_name | groupName | |
| join_time | joinTime | |
| create_time | createTime | |
| update_time | updateTime | |

MyBatis-Plus 全局配置 `map-underscore-to-camel-case: true` 即可自动完成映射。

---

## 7. Redis 缓存设计

| Key 模式 | Value | TTL | 用途 |
|----------|-------|-----|------|
| `user:token:{userId}` | JWT Token | 7天 | Token 黑名单/续期（预留） |
| `checkin:today:{userId}` | 今日打卡状态 JSON | 当日剩余时间 | 快速判断今日是否已打卡 |
| `group:invite:{inviteCode}` | groupId | 永不过期 | 邀请码快速查询（预留） |

> MVP 阶段 Redis 为可选优化项，核心逻辑不强依赖 Redis。数据库唯一索引已保证数据一致性。
