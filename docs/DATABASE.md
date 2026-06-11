# ArchStudyCheckin 数据库设计文档

## 1. 数据库基本信息

| 项目 | 说明 |
|------|------|
| 数据库 | MySQL 8.0+ |
| 字符集 | utf8mb4 |
| 排序规则 | utf8mb4_general_ci |
| 存储引擎 | InnoDB |
| 命名规范 | 表名、字段名均使用下划线命名（snake_case） |
| 主键策略 | BIGINT，由 MyBatis-Plus `IdType.ASSIGN_ID`（雪花算法）生成，不使用 AUTO_INCREMENT |
| 时间字段 | 统一使用 DATETIME NOT NULL |
| 逻辑删除 | 使用 `deleted` 字段（0=未删除，1=已删除） |
| 公共字段 | 所有表包含 `created_at`、`updated_at`、`deleted` |
| 外键策略 | 所有外键仅做逻辑约束，不创建数据库物理外键 |

---

## 2. ER 关系概览

```
app_user (1) ──── (N) study_plan
app_user (1) ──── (N) checkin_record
study_plan (1) ──── (N) checkin_record
app_user (1) ──── (N) study_group (创建关系，owner_id)
app_user (N) ──── (N) study_group (通过 study_group_member 多对多)
app_user (1) ──── (1) reminder_setting
```

---

## 3. 表结构设计

### 3.1 用户表 `app_user`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 用户ID（雪花算法） |
| username | VARCHAR(20) | NOT NULL, UNIQUE | 用户名 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 邮箱 |
| nickname | VARCHAR(64) | NOT NULL | 昵称 |
| password | VARCHAR(100) | NOT NULL | 密码（BCrypt加密） |
| avatar_url | VARCHAR(255) | NULL | 头像URL（预留） |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1正常，0禁用 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `uk_email` UNIQUE (email)
- `uk_username` UNIQUE (username)

**唯一索引说明：**
- email 和 username 均为 NOT NULL，不存在 MySQL 多 NULL 场景。
- 若后续新增 phone 字段且允许为空，MySQL 中 UNIQUE 索引允许多个 NULL 值共存（InnoDB），即多个用户 phone 为 NULL 不违反唯一约束。若需要"非空时唯一"的语义，需在业务层额外校验。

---

### 3.2 学习计划表 `study_plan`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 计划ID |
| user_id | BIGINT | NOT NULL | 所属用户ID（逻辑外键 → app_user.id） |
| plan_name | VARCHAR(50) | NOT NULL | 计划名称 |
| subject | VARCHAR(50) | NOT NULL | 科目（如"系统架构设计"） |
| target_days | INT | NOT NULL | 目标天数 |
| start_date | DATE | NOT NULL | 开始日期 |
| daily_target_minutes | INT | NULL | 每日目标学习分钟数 |
| weekly_target_days | INT | NULL | 每周目标学习天数（1-7） |
| stage | VARCHAR(20) | NULL | 当前备考阶段（如"基础阶段"） |
| status | TINYINT | NOT NULL, DEFAULT 0 | 状态：0=进行中，1=已完成，2=已放弃 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `idx_user_id` (user_id)
- `idx_user_status` (user_id, status)

---

### 3.3 打卡记录表 `checkin_record`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 记录ID |
| user_id | BIGINT | NOT NULL | 用户ID（逻辑外键 → app_user.id） |
| plan_id | BIGINT | NOT NULL | 学习计划ID（逻辑外键 → study_plan.id） |
| checkin_date | DATE | NOT NULL | 打卡日期（用户本地时区） |
| duration | INT | NOT NULL | 学习时长（分钟） |
| content | VARCHAR(200) | NULL | 学习内容摘要 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `uk_user_plan_date` UNIQUE (user_id, plan_id, checkin_date) — 保证打卡幂等
- `idx_user_date` (user_id, checkin_date) — 查询今日/月度打卡
- `idx_plan_id` (plan_id)

**幂等与逻辑删除说明：**
- 唯一索引 `uk_user_plan_date` 保证同一用户同一天同一计划只有一条记录。
- 第一版不提供打卡删除功能（物理删除或逻辑删除），`deleted` 字段保留但始终为 0。
- 重复提交打卡时，业务层先根据唯一键查询已有记录，若存在则 UPDATE（duration、content、updated_at），不新增记录。
- 由于第一版 deleted 始终为 0，唯一索引与逻辑删除无冲突。若后续需支持删除，可改用联合唯一索引包含 deleted 字段，或物理删除方案。

---

### 3.4 学习小组表 `study_group`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 小组ID |
| group_name | VARCHAR(30) | NOT NULL | 小组名称 |
| description | VARCHAR(200) | NULL | 小组简介 |
| invite_code | VARCHAR(6) | NOT NULL, UNIQUE | 邀请码（6位大写字母+数字） |
| owner_id | BIGINT | NOT NULL | 组长用户ID（逻辑外键 → app_user.id） |
| member_count | INT | NOT NULL, DEFAULT 1 | 当前成员数 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `uk_invite_code` UNIQUE (invite_code)
- `idx_owner_id` (owner_id)

---

### 3.5 小组成员表 `study_group_member`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 记录ID |
| group_id | BIGINT | NOT NULL | 小组ID（逻辑外键 → study_group.id） |
| user_id | BIGINT | NOT NULL | 用户ID（逻辑外键 → app_user.id） |
| role | TINYINT | NOT NULL, DEFAULT 0 | 角色：0=普通成员，1=组长 |
| join_time | DATETIME | NOT NULL | 加入时间 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `uk_group_user` UNIQUE (group_id, user_id) — 防止重复加入
- `idx_user_id` (user_id) — 查询用户所在小组
- `idx_group_id` (group_id) — 查询小组成员列表

**逻辑删除与唯一索引说明：**
- 退出小组时逻辑删除（deleted=1），若用户重新加入同一小组，需先物理删除旧记录或使用 `INSERT ... ON DUPLICATE KEY UPDATE deleted=0` 恢复。
- 推荐方案：退出时物理删除 study_group_member 记录（此表不使用逻辑删除），保持唯一索引有效。

---

### 3.6 提醒设置表 `reminder_setting`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, NOT NULL | 记录ID |
| user_id | BIGINT | NOT NULL, UNIQUE | 用户ID（逻辑外键 → app_user.id） |
| remind_time | TIME | NOT NULL, DEFAULT '20:00:00' | 提醒时间（HH:mm:ss） |
| enabled | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否启用（0=关闭，1=启用） |
| repeat_type | TINYINT | NOT NULL, DEFAULT 0 | 重复类型：0=每天，1=工作日，2=自定义 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT(1) | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引：**
- `uk_user_id` UNIQUE (user_id) — 每用户一条提醒配置

**说明：**
- 第一版 iOS 使用本地通知实现提醒，此表预留用于后续跨设备同步和 APNs 远程推送扩展。
- 用户首次设置提醒时 INSERT，后续修改 UPDATE。

---

## 4. DDL 建表语句

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS arch_study_checkin
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE arch_study_checkin;

-- 用户表
CREATE TABLE `app_user` (
  `id` BIGINT NOT NULL COMMENT '用户ID(雪花算法)',
  `username` VARCHAR(20) NOT NULL COMMENT '用户名',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL(预留)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常,0禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- 学习计划表
CREATE TABLE `study_plan` (
  `id` BIGINT NOT NULL COMMENT '计划ID(雪花算法)',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `plan_name` VARCHAR(50) NOT NULL COMMENT '计划名称',
  `subject` VARCHAR(50) NOT NULL COMMENT '科目',
  `target_days` INT NOT NULL COMMENT '目标天数',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `daily_target_minutes` INT DEFAULT NULL COMMENT '每日目标学习分钟数',
  `weekly_target_days` INT DEFAULT NULL COMMENT '每周目标学习天数(1-7)',
  `stage` VARCHAR(20) DEFAULT NULL COMMENT '当前备考阶段',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0=进行中,1=已完成,2=已放弃)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学习计划表';

-- 打卡记录表
CREATE TABLE `checkin_record` (
  `id` BIGINT NOT NULL COMMENT '记录ID(雪花算法)',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `plan_id` BIGINT NOT NULL COMMENT '学习计划ID',
  `checkin_date` DATE NOT NULL COMMENT '打卡日期(用户本地时区)',
  `duration` INT NOT NULL COMMENT '学习时长(分钟)',
  `content` VARCHAR(200) DEFAULT NULL COMMENT '学习内容摘要',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_plan_date` (`user_id`, `plan_id`, `checkin_date`),
  KEY `idx_user_date` (`user_id`, `checkin_date`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='打卡记录表';

-- 学习小组表
CREATE TABLE `study_group` (
  `id` BIGINT NOT NULL COMMENT '小组ID(雪花算法)',
  `group_name` VARCHAR(30) NOT NULL COMMENT '小组名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '小组简介',
  `invite_code` VARCHAR(6) NOT NULL COMMENT '邀请码(6位大写字母+数字)',
  `owner_id` BIGINT NOT NULL COMMENT '组长用户ID',
  `member_count` INT NOT NULL DEFAULT 1 COMMENT '成员数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学习小组表';

-- 小组成员表
CREATE TABLE `study_group_member` (
  `id` BIGINT NOT NULL COMMENT '记录ID(雪花算法)',
  `group_id` BIGINT NOT NULL COMMENT '小组ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色(0=成员,1=组长)',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='小组成员表';

-- 提醒设置表
CREATE TABLE `reminder_setting` (
  `id` BIGINT NOT NULL COMMENT '记录ID(雪花算法)',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `remind_time` TIME NOT NULL DEFAULT '20:00:00' COMMENT '提醒时间',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用(0=关闭,1=启用)',
  `repeat_type` TINYINT NOT NULL DEFAULT 0 COMMENT '重复类型(0=每天,1=工作日,2=自定义)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=否,1=是)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='提醒设置表';
```

---

## 5. 主键策略说明

| 项目 | 说明 |
|------|------|
| 类型 | BIGINT（64位） |
| 生成方式 | MyBatis-Plus `IdType.ASSIGN_ID`（内置雪花算法） |
| DDL | 不使用 AUTO_INCREMENT |
| 优势 | 分布式友好、无中心依赖、有序递增、不暴露业务量 |

Java 实体配置示例：
```java
@TableId(type = IdType.ASSIGN_ID)
private Long id;
```

---

## 6. 逻辑删除策略说明

| 表 | deleted 字段 | 逻辑删除行为 |
|------|-------------|-------------|
| app_user | 保留 | 预留，第一版不实现注销 |
| study_plan | 保留 | 预留，第一版不实现删除计划 |
| checkin_record | 保留 | **第一版不提供删除功能**，deleted 始终为 0，避免与 uk_user_plan_date 冲突 |
| study_group | 保留 | 解散小组时 deleted=1 |
| study_group_member | 保留 | **推荐退出时物理删除**，保持 uk_group_user 唯一索引有效 |
| reminder_setting | 保留 | 预留 |

**唯一索引与逻辑删除冲突分析：**
- `checkin_record.uk_user_plan_date`：第一版 deleted 始终为 0，无冲突。
- `study_group_member.uk_group_user`：退出小组采用物理删除，重新加入直接 INSERT，无冲突。
- `study_group.uk_invite_code`：解散后邀请码仍占用，不可被新小组使用。若需复用，后续可改为物理删除或在邀请码生成时排除已用码。

MyBatis-Plus 全局配置：
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: ASSIGN_ID
```

---

## 7. 数据风险说明

| 操作 | 风险等级 | 说明 |
|------|----------|------|
| 用户注册 | 低 | INSERT app_user，唯一索引防止重复注册 |
| 创建学习计划 | 低 | INSERT study_plan，无唯一约束冲突风险 |
| 打卡（新增/更新） | 中 | 唯一索引保证幂等，重复提交走 UPDATE 逻辑，不会丢失首次打卡时间 |
| 创建小组 | 低 | INSERT study_group + study_group_member，邀请码唯一校验 |
| 加入小组 | 中 | INSERT study_group_member + UPDATE member_count，需事务保证一致性 |
| 退出小组 | 中 | 物理删除 study_group_member + UPDATE member_count，需事务 |
| 解散小组 | 高 | 逻辑删除 study_group + 物理删除所有 study_group_member，仅组长可操作 |
| 设置提醒 | 低 | INSERT/UPDATE reminder_setting，唯一索引保证每用户一条 |

---

## 8. 字段映射说明（数据库 → Java）

| 数据库字段（下划线） | Java 字段（驼峰） | 说明 |
|---------------------|-------------------|------|
| user_id | userId | MyBatis-Plus 自动映射 |
| plan_name | planName | |
| checkin_date | checkinDate | |
| target_days | targetDays | |
| start_date | startDate | |
| daily_target_minutes | dailyTargetMinutes | |
| weekly_target_days | weeklyTargetDays | |
| stage | stage | 无需映射，名称相同 |
| invite_code | inviteCode | |
| owner_id | ownerId | |
| member_count | memberCount | |
| group_name | groupName | |
| join_time | joinTime | |
| remind_time | remindTime | |
| repeat_type | repeatType | |
| created_at | createTime | |
| updated_at | updateTime | |

MyBatis-Plus 全局配置 `map-underscore-to-camel-case: true` 即可自动完成映射。

---

## 9. Redis 缓存设计

| Key 模式 | Value | TTL | 用途 |
|----------|-------|-----|------|
| `user:token:{userId}` | JWT Token | 7天 | Token 黑名单/续期（预留） |
| `checkin:today:{userId}` | 今日打卡状态 JSON | 当日剩余时间 | 快速判断今日是否已打卡 |
| `group:invite:{inviteCode}` | groupId | 永不过期 | 邀请码快速查询（预留） |

> MVP 阶段 Redis 为可选优化项，核心逻辑不强依赖 Redis。数据库唯一索引已保证数据一致性。
