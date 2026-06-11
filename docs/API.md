# ArchStudyCheckin 接口文档

## 1. 接口规范

### 1.1 基础信息

| 项目 | 说明 |
|------|------|
| 基础路径 | `/api/v1` |
| 协议 | HTTPS（开发环境可 HTTP） |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 鉴权方式 | JWT ****** |
| 文档工具 | Knife4j / OpenAPI 3.0 |

### 1.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 业务状态码，200=成功 |
| message | String | 提示信息 |
| data | Object/Array/null | 响应数据 |

### 1.3 业务状态码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录或 Token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如重复注册） |
| 500 | 服务器内部错误 |

### 1.4 鉴权说明

- 登录成功后返回 JWT Token。
- 后续请求在 Header 中携带：`Authorization: ******
- Token 有效期 7 天，过期返回 401。
- userId 从 Token 中解析，前端不传 userId。

### 1.5 分页参数（预留）

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| pageNum | Integer | 1 | 页码 |
| pageSize | Integer | 20 | 每页数量（最大100） |

---

## 2. 认证模块

### 2.1 用户注册

**POST** `/api/v1/auth/register`

**鉴权：** 无需

**请求体：**
```json
{
  "username": "zhangsan",
  "email": "zhangsan@example.com",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| username | String | 是 | 2-20字符 |
| email | String | 是 | 邮箱格式 |
| password | String | 是 | 6-20字符 |

**成功响应：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "******",
    "userId": "1800000000000001",
    "username": "zhangsan",
    "email": "zhangsan@example.com"
  }
}
```

**错误场景：**
| code | message |
|------|---------|
| 400 | 参数校验失败 |
| 409 | 邮箱已注册 / 用户名已存在 |

**风险说明：** INSERT app_user，唯一索引防止重复注册。

---

### 2.2 用户登录

**POST** `/api/v1/auth/login`

**鉴权：** 无需

**请求体：**
```json
{
  "email": "zhangsan@example.com",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| email | String | 是 | 邮箱格式 |
| password | String | 是 | 6-20字符 |

**成功响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "******",
    "userId": "1800000000000001",
    "username": "zhangsan",
    "email": "zhangsan@example.com"
  }
}
```

**错误场景：**
| code | message |
|------|---------|
| 400 | 参数校验失败 |
| 401 | 邮箱或密码错误 |

---

### 2.3 获取当前用户信息

**GET** `/api/v1/auth/me`

**鉴权：** 需要

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "1800000000000001",
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "avatarUrl": null
  }
}
```

---

## 3. 学习计划模块

### 3.1 创建学习计划

**POST** `/api/v1/plans`

**鉴权：** 需要

**请求体：**
```json
{
  "planName": "系统架构师备考",
  "subject": "系统架构设计",
  "targetDays": 90,
  "startDate": "2026-06-15"
}
```

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| planName | String | 是 | 1-50字符 |
| subject | String | 是 | 1-50字符 |
| targetDays | Integer | 是 | 1-365 |
| startDate | String | 是 | yyyy-MM-dd 格式 |

**成功响应：**
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": "1800000000000010",
    "planName": "系统架构师备考",
    "subject": "系统架构设计",
    "targetDays": 90,
    "startDate": "2026-06-15",
    "status": 0
  }
}
```

**风险说明：** INSERT study_plan，无唯一约束冲突风险。

---

### 3.2 查询我的学习计划列表

**GET** `/api/v1/plans`

**鉴权：** 需要

**查询参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 否 | 按状态筛选（0/1/2） |

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "1800000000000010",
      "planName": "系统架构师备考",
      "subject": "系统架构设计",
      "targetDays": 90,
      "startDate": "2026-06-15",
      "status": 0,
      "createTime": "2026-06-15T10:00:00"
    }
  ]
}
```

---

### 3.3 查询学习计划详情

**GET** `/api/v1/plans/{planId}`

**鉴权：** 需要

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "1800000000000010",
    "planName": "系统架构师备考",
    "subject": "系统架构设计",
    "targetDays": 90,
    "startDate": "2026-06-15",
    "status": 0,
    "createTime": "2026-06-15T10:00:00"
  }
}
```

**错误场景：**
| code | message |
|------|---------|
| 404 | 计划不存在 |
| 403 | 无权查看他人计划 |

---

## 4. 打卡模块

### 4.1 提交打卡（幂等）

**POST** `/api/v1/checkins`

**鉴权：** 需要

**请求体：**
```json
{
  "planId": "1800000000000010",
  "checkinDate": "2026-06-15",
  "duration": 120,
  "content": "学习了软件架构风格章节"
}
```

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| planId | Long | 是 | 必须是当前用户的计划 |
| checkinDate | String | 是 | yyyy-MM-dd 格式，用户本地日期 |
| duration | Integer | 是 | 1-1440（分钟） |
| content | String | 否 | 最多200字符 |

**成功响应（首次打卡）：**
```json
{
  "code": 200,
  "message": "打卡成功",
  "data": {
    "id": "1800000000000020",
    "planId": "1800000000000010",
    "checkinDate": "2026-06-15",
    "duration": 120,
    "content": "学习了软件架构风格章节",
    "isNew": true,
    "createTime": "2026-06-15T22:30:00"
  }
}
```

**成功响应（重复提交，更新）：**
```json
{
  "code": 200,
  "message": "打卡已更新",
  "data": {
    "id": "1800000000000020",
    "planId": "1800000000000010",
    "checkinDate": "2026-06-15",
    "duration": 150,
    "content": "补充学习了设计模式",
    "isNew": false,
    "updateTime": "2026-06-15T23:00:00"
  }
}
```

**错误场景：**
| code | message |
|------|---------|
| 400 | 参数校验失败 |
| 404 | 计划不存在 |
| 403 | 该计划不属于当前用户 |

**幂等说明：**
- 同一 userId + planId + checkinDate 只有一条记录。
- 首次提交 INSERT，再次提交 UPDATE（duration, content）。
- 通过 `isNew` 字段告知客户端是新建还是更新。

**风险说明：** 唯一索引保证幂等。重复提交会覆盖已有的 duration 和 content。

---

### 4.2 查看今日打卡状态

**GET** `/api/v1/checkins/today`

**鉴权：** 需要

**查询参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| date | String | 是 | 用户本地日期 yyyy-MM-dd |

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "planId": "1800000000000010",
      "planName": "系统架构师备考",
      "checkedIn": true,
      "duration": 120,
      "content": "学习了软件架构风格章节",
      "checkinTime": "2026-06-15T22:30:00"
    },
    {
      "planId": "1800000000000011",
      "planName": "英语学习",
      "checkedIn": false,
      "duration": null,
      "content": null,
      "checkinTime": null
    }
  ]
}
```

---

### 4.3 查看月度打卡记录

**GET** `/api/v1/checkins/monthly`

**鉴权：** 需要

**查询参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| yearMonth | String | 是 | 格式 yyyy-MM（如 2026-06） |

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "yearMonth": "2026-06",
    "checkinDays": [1, 3, 5, 6, 7, 10, 11, 12, 15],
    "totalDays": 9,
    "details": [
      {
        "checkinDate": "2026-06-15",
        "totalDuration": 180,
        "planCount": 2
      }
    ]
  }
}
```

**说明：**
- `checkinDays`：当月有打卡的日期列表（天数），供日历 UI 标记。
- `details`：每个打卡日的汇总（总时长、打卡计划数）。

---

### 4.4 查看个人统计

**GET** `/api/v1/checkins/stats`

**鉴权：** 需要

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCheckinDays": 45,
    "consecutiveDays": 7,
    "totalDuration": 5400,
    "totalPlans": 3,
    "activePlans": 2
  }
}
```

| 字段 | 说明 |
|------|------|
| totalCheckinDays | 累计打卡天数（去重日期） |
| consecutiveDays | 当前连续打卡天数 |
| totalDuration | 总学习时长（分钟） |
| totalPlans | 总计划数 |
| activePlans | 进行中计划数 |

---

## 5. 小组模块

### 5.1 创建学习小组

**POST** `/api/v1/groups`

**鉴权：** 需要

**请求体：**
```json
{
  "groupName": "架构师冲刺小组",
  "description": "一起备考系统架构设计师"
}
```

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| groupName | String | 是 | 1-30字符 |
| description | String | 否 | 最多200字符 |

**成功响应：**
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": "1800000000000030",
    "groupName": "架构师冲刺小组",
    "description": "一起备考系统架构设计师",
    "inviteCode": "A3B7K9",
    "memberCount": 1
  }
}
```

**错误场景：**
| code | message |
|------|---------|
| 400 | 参数校验失败 |
| 409 | 已达最大创建数量（3个） |

**风险说明：** INSERT study_group + study_group_member（组长），事务操作。邀请码生成时校验全局唯一。

---

### 5.2 通过邀请码加入小组

**POST** `/api/v1/groups/join`

**鉴权：** 需要

**请求体：**
```json
{
  "inviteCode": "A3B7K9"
}
```

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| inviteCode | String | 是 | 6位大写字母+数字 |

**成功响应：**
```json
{
  "code": 200,
  "message": "加入成功",
  "data": {
    "groupId": "1800000000000030",
    "groupName": "架构师冲刺小组",
    "memberCount": 5
  }
}
```

**错误场景：**
| code | message |
|------|---------|
| 400 | 邀请码格式错误 |
| 404 | 邀请码无效或小组不存在 |
| 409 | 已是该小组成员 / 已达最大加入数量（5个） |

**风险说明：** INSERT study_group_member + UPDATE member_count，需事务保证一致性。唯一索引防止重复加入。

---

### 5.3 查看我的小组列表

**GET** `/api/v1/groups`

**鉴权：** 需要

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "1800000000000030",
      "groupName": "架构师冲刺小组",
      "description": "一起备考系统架构设计师",
      "inviteCode": "A3B7K9",
      "memberCount": 5,
      "role": 1,
      "joinTime": "2026-06-10T10:00:00"
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| role | 0=成员，1=组长 |

---

### 5.4 查看小组详情

**GET** `/api/v1/groups/{groupId}`

**鉴权：** 需要（必须是小组成员）

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "1800000000000030",
    "groupName": "架构师冲刺小组",
    "description": "一起备考系统架构设计师",
    "inviteCode": "A3B7K9",
    "ownerId": "1800000000000001",
    "memberCount": 5,
    "members": [
      {
        "userId": "1800000000000001",
        "username": "zhangsan",
        "role": 1,
        "joinTime": "2026-06-10T10:00:00"
      }
    ]
  }
}
```

**错误场景：**
| code | message |
|------|---------|
| 403 | 非小组成员，无权查看 |
| 404 | 小组不存在 |

---

### 5.5 查看小组今日打卡情况

**GET** `/api/v1/groups/{groupId}/checkins/today`

**鉴权：** 需要（必须是小组成员）

**查询参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| date | String | 是 | 用户本地日期 yyyy-MM-dd |

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "groupId": "1800000000000030",
    "groupName": "架构师冲刺小组",
    "date": "2026-06-15",
    "totalMembers": 5,
    "checkedInCount": 3,
    "members": [
      {
        "userId": "1800000000000001",
        "username": "zhangsan",
        "checkedIn": true,
        "totalDuration": 180,
        "checkinTime": "2026-06-15T22:30:00"
      },
      {
        "userId": "1800000000000002",
        "username": "lisi",
        "checkedIn": false,
        "totalDuration": 0,
        "checkinTime": null
      }
    ]
  }
}
```

**错误场景：**
| code | message |
|------|---------|
| 403 | 非小组成员，无权查看 |
| 404 | 小组不存在 |

**权限说明：** 后端校验当前用户是否为该小组成员（查询 study_group_member），非成员返回 403。

---

### 5.6 退出小组

**POST** `/api/v1/groups/{groupId}/quit`

**鉴权：** 需要（必须是小组成员且非组长）

**成功响应：**
```json
{
  "code": 200,
  "message": "退出成功",
  "data": null
}
```

**错误场景：**
| code | message |
|------|---------|
| 403 | 组长不能退出，请先解散小组 |
| 404 | 小组不存在或非小组成员 |

**风险说明：** 物理删除 study_group_member + UPDATE member_count，需事务。

---

### 5.7 解散小组

**POST** `/api/v1/groups/{groupId}/dismiss`

**鉴权：** 需要（必须是组长）

**成功响应：**
```json
{
  "code": 200,
  "message": "解散成功",
  "data": null
}
```

**错误场景：**
| code | message |
|------|---------|
| 403 | 非组长，无权解散 |
| 404 | 小组不存在 |

**风险说明：** 逻辑删除 study_group + 物理删除所有 study_group_member，需事务。此操作不可逆。

---

## 6. 提醒设置模块

### 6.1 获取提醒设置

**GET** `/api/v1/reminder`

**鉴权：** 需要

**成功响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "remindTime": "20:00",
    "enabled": true,
    "repeatType": 0
  }
}
```

**说明：** 若用户无提醒设置记录，返回默认值。

---

### 6.2 保存提醒设置

**PUT** `/api/v1/reminder`

**鉴权：** 需要

**请求体：**
```json
{
  "remindTime": "21:00",
  "enabled": true,
  "repeatType": 0
}
```

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| remindTime | String | 是 | HH:mm 格式 |
| enabled | Boolean | 是 | |
| repeatType | Integer | 是 | 0=每天，1=工作日，2=自定义 |

**成功响应：**
```json
{
  "code": 200,
  "message": "保存成功",
  "data": {
    "remindTime": "21:00",
    "enabled": true,
    "repeatType": 0
  }
}
```

**风险说明：** INSERT 或 UPDATE reminder_setting，唯一索引保证每用户一条。

---

## 7. 接口汇总表

| 序号 | 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|------|
| 1 | POST | /api/v1/auth/register | 注册 | 否 |
| 2 | POST | /api/v1/auth/login | 登录 | 否 |
| 3 | GET | /api/v1/auth/me | 当前用户信息 | 是 |
| 4 | POST | /api/v1/plans | 创建学习计划 | 是 |
| 5 | GET | /api/v1/plans | 我的计划列表 | 是 |
| 6 | GET | /api/v1/plans/{planId} | 计划详情 | 是 |
| 7 | POST | /api/v1/checkins | 提交打卡 | 是 |
| 8 | GET | /api/v1/checkins/today | 今日打卡状态 | 是 |
| 9 | GET | /api/v1/checkins/monthly | 月度打卡记录 | 是 |
| 10 | GET | /api/v1/checkins/stats | 个人统计 | 是 |
| 11 | POST | /api/v1/groups | 创建小组 | 是 |
| 12 | POST | /api/v1/groups/join | 加入小组 | 是 |
| 13 | GET | /api/v1/groups | 我的小组列表 | 是 |
| 14 | GET | /api/v1/groups/{groupId} | 小组详情 | 是 |
| 15 | GET | /api/v1/groups/{groupId}/checkins/today | 小组今日打卡 | 是 |
| 16 | POST | /api/v1/groups/{groupId}/quit | 退出小组 | 是 |
| 17 | POST | /api/v1/groups/{groupId}/dismiss | 解散小组 | 是 |
| 18 | GET | /api/v1/reminder | 获取提醒设置 | 是 |
| 19 | PUT | /api/v1/reminder | 保存提醒设置 | 是 |

---

## 8. 安全与权限要求

1. **身份认证**：除注册和登录外，所有接口需携带有效 JWT Token。
2. **身份来源**：userId 从 Token 解析，后端不接受前端传入 userId。
3. **数据隔离**：用户只能操作自己的计划和打卡记录。
4. **小组权限**：查看小组数据接口需校验当前用户是否为小组成员。
5. **组长权限**：解散小组仅组长可操作。
6. **退出限制**：组长不可退出，需先解散。
7. **参数校验**：所有接口使用 jakarta.validation 进行入参校验。
8. **统一异常**：全局异常处理器捕获异常并返回统一 Result 格式。
