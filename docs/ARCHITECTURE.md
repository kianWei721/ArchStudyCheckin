# ArchStudyCheckin 架构设计文档

## 1. 系统架构概览

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    iOS App (SwiftUI)                      │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │  Views  │ │ViewModels│ │ Services │ │  Keychain  │  │
│  └─────────┘ └──────────┘ └──────────┘ └────────────┘  │
└────────────────────────────┬────────────────────────────┘
                             │ HTTPS / JSON
                             ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Boot 3.x Backend                     │
│  ┌──────────────────────────────────────────────────┐   │
│  │              Controller Layer                     │   │
│  │  (REST API + 参数校验 + Knife4j 文档)            │   │
│  ├──────────────────────────────────────────────────┤   │
│  │              Service Layer                        │   │
│  │  (业务逻辑 + 事务管理 + 权限校验)                │   │
│  ├──────────────────────────────────────────────────┤   │
│  │              Mapper Layer (MyBatis-Plus)          │   │
│  │  (数据访问 + 自动 CRUD)                          │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │ Security │  │   JWT    │  │ Global Exception     │  │
│  │ Filter   │  │  Utils   │  │ Handler              │  │
│  └──────────┘  └──────────┘  └──────────────────────┘  │
└──────────┬──────────────────────────────┬───────────────┘
           │                              │
           ▼                              ▼
    ┌─────────────┐              ┌─────────────┐
    │   MySQL 8   │              │    Redis    │
    │  (主存储)   │              │  (缓存/预留) │
    └─────────────┘              └─────────────┘
```

### 1.2 技术选型

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| iOS 前端 | SwiftUI | iOS 16+ | MVVM 架构 |
| 后端框架 | Spring Boot | 3.x | Java 17 |
| ORM | MyBatis-Plus | 3.5.x | 简化 CRUD |
| 数据库 | MySQL | 8.0+ | 主存储 |
| 缓存 | Redis | 7.x | 预留优化 |
| 安全 | Spring Security + JWT | - | 无状态认证 |
| API 文档 | Knife4j (OpenAPI 3.0) | 4.x | 在线调试 |
| 构建工具 | Maven | 3.9+ | 依赖管理 |
| Java | JDK | 17 | LTS 版本 |

---

## 2. 后端架构设计

### 2.1 项目结构

```
backend/
├── pom.xml
├── src/main/java/com/archstudy/checkin/
│   ├── ArchStudyCheckinApplication.java          # 启动类
│   ├── config/                                    # 配置层
│   │   ├── SecurityConfig.java                   # Spring Security 配置
│   │   ├── MybatisPlusConfig.java                # MyBatis-Plus 配置
│   │   ├── RedisConfig.java                      # Redis 配置（预留）
│   │   ├── Knife4jConfig.java                    # API 文档配置
│   │   └── WebMvcConfig.java                     # Web 配置（CORS 等）
│   ├── common/                                    # 公共组件
│   │   ├── result/
│   │   │   ├── Result.java                       # 统一响应类
│   │   │   └── ResultCode.java                   # 状态码枚举
│   │   ├── exception/
│   │   │   ├── BusinessException.java            # 业务异常
│   │   │   └── GlobalExceptionHandler.java       # 全局异常处理
│   │   └── utils/
│   │       └── JwtUtils.java                     # JWT 工具类
│   ├── security/                                  # 安全模块
│   │   ├── JwtAuthenticationFilter.java          # JWT 过滤器
│   │   ├── UserDetailsServiceImpl.java           # 用户认证服务
│   │   └── SecurityUtils.java                    # 获取当前登录用户
│   ├── controller/                                # 控制器层
│   │   ├── AuthController.java                   # 认证接口
│   │   ├── StudyPlanController.java              # 学习计划接口
│   │   ├── CheckinController.java                # 打卡接口
│   │   ├── GroupController.java                  # 小组接口
│   │   └── ReminderController.java               # 提醒设置接口
│   ├── service/                                   # 服务层
│   │   ├── AuthService.java
│   │   ├── StudyPlanService.java
│   │   ├── CheckinService.java
│   │   ├── GroupService.java
│   │   └── ReminderService.java
│   ├── service/impl/                              # 服务实现
│   │   ├── AuthServiceImpl.java
│   │   ├── StudyPlanServiceImpl.java
│   │   ├── CheckinServiceImpl.java
│   │   ├── GroupServiceImpl.java
│   │   └── ReminderServiceImpl.java
│   ├── mapper/                                    # 数据访问层
│   │   ├── AppUserMapper.java
│   │   ├── StudyPlanMapper.java
│   │   ├── CheckinRecordMapper.java
│   │   ├── StudyGroupMapper.java
│   │   ├── StudyGroupMemberMapper.java
│   │   └── ReminderSettingMapper.java
│   ├── entity/                                    # 实体类
│   │   ├── AppUser.java
│   │   ├── StudyPlan.java
│   │   ├── CheckinRecord.java
│   │   ├── StudyGroup.java
│   │   ├── StudyGroupMember.java
│   │   └── ReminderSetting.java
│   ├── dto/                                       # 请求 DTO
│   │   ├── auth/
│   │   │   ├── RegisterRequest.java
│   │   │   └── LoginRequest.java
│   │   ├── plan/
│   │   │   ├── CreatePlanRequest.java
│   │   │   └── UpdatePlanRequest.java
│   │   ├── checkin/
│   │   │   └── CheckinRequest.java
│   │   ├── group/
│   │   │   ├── CreateGroupRequest.java
│   │   │   └── JoinGroupRequest.java
│   │   └── reminder/
│   │       └── ReminderRequest.java
│   └── vo/                                        # 响应 VO
│       ├── auth/
│       │   ├── LoginVO.java
│       │   └── UserInfoVO.java
│       ├── plan/
│       │   └── StudyPlanVO.java
│       ├── checkin/
│       │   ├── CheckinVO.java
│       │   ├── TodayCheckinVO.java
│       │   ├── MonthlyCheckinVO.java
│       │   └── CheckinStatsVO.java
│       ├── group/
│       │   ├── GroupVO.java
│       │   ├── GroupDetailVO.java
│       │   └── GroupCheckinVO.java
│       └── reminder/
│           └── ReminderVO.java
├── src/main/resources/
│   ├── application.yml                            # 主配置
│   ├── application-dev.yml                        # 开发环境配置
│   ├── application-prod.yml                       # 生产环境配置
│   └── mapper/                                    # MyBatis XML（复杂查询）
│       └── CheckinRecordMapper.xml
└── src/test/java/                                 # 单元测试
```

### 2.2 分层职责

| 层级 | 职责 | 规范 |
|------|------|------|
| Controller | 接收请求、参数校验、调用 Service、返回响应 | 不包含业务逻辑 |
| Service | 业务逻辑、事务管理、权限校验 | 接口 + 实现分离 |
| Mapper | 数据库操作 | 继承 BaseMapper，复杂 SQL 用 XML |
| Entity | 数据库表映射 | 对应表结构，使用 MyBatis-Plus 注解 |
| DTO | 请求参数封装 | 包含 jakarta.validation 校验注解 |
| VO | 响应数据封装 | 仅包含前端需要的字段 |

### 2.3 安全架构

```
Request → SecurityFilter → JwtAuthenticationFilter → Controller
                                    │
                                    ▼
                          解析 JWT Token
                          提取 userId
                          设置 SecurityContext
                                    │
                                    ▼
                          Controller/Service
                          通过 SecurityUtils.getCurrentUserId()
                          获取当前登录用户ID
```

**关键安全设计：**
1. `JwtAuthenticationFilter`：拦截所有需鉴权请求，解析 Token，设置 Authentication。
2. `SecurityUtils.getCurrentUserId()`：从 SecurityContext 中获取当前用户 ID，所有 Service 层通过此方法获取 userId。
3. 白名单路径：`/api/auth/register`、`/api/auth/login`、Knife4j 文档路径。
4. 权限校验在 Service 层完成，确保操作资源归属当前用户。

### 2.4 统一响应与异常处理

```java
// 统一响应
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
}

// 全局异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 处理参数校验异常 → 400
    // 处理业务异常 → 自定义 code
    // 处理认证异常 → 401
    // 处理未知异常 → 500
}
```

### 2.5 打卡幂等实现方案

```
POST /api/checkins
       │
       ▼
  参数校验（日期格式、范围）
       │
       ▼
  校验 planId 归属当前用户
       │
       ▼
  查询 checkin_record WHERE user_id + plan_id + checkin_date
       │
       ├── 不存在 → INSERT → 返回 isNew=true
       │
       └── 已存在 → UPDATE duration, content → 返回 isNew=false
```

---

## 3. iOS 架构设计

### 3.1 项目结构

```
ios/ArchStudyCheckin/
├── ArchStudyCheckinApp.swift                      # App 入口
├── Info.plist
├── Models/                                        # 数据模型
│   ├── User.swift
│   ├── StudyPlan.swift
│   ├── CheckinRecord.swift
│   ├── StudyGroup.swift
│   └── ReminderSetting.swift
├── ViewModels/                                    # ViewModel（MVVM）
│   ├── AuthViewModel.swift
│   ├── HomeViewModel.swift
│   ├── PlanViewModel.swift
│   ├── CheckinViewModel.swift
│   ├── CalendarViewModel.swift
│   ├── StatsViewModel.swift
│   ├── GroupViewModel.swift
│   └── ProfileViewModel.swift
├── Views/                                         # SwiftUI 视图
│   ├── Auth/
│   │   ├── LoginView.swift
│   │   └── RegisterView.swift
│   ├── Home/
│   │   └── HomeView.swift
│   ├── Checkin/
│   │   └── CheckinView.swift
│   ├── Calendar/
│   │   └── CalendarView.swift
│   ├── Stats/
│   │   └── StatsView.swift
│   ├── Group/
│   │   ├── GroupListView.swift
│   │   ├── GroupDetailView.swift
│   │   ├── CreateGroupView.swift
│   │   └── JoinGroupView.swift
│   ├── Profile/
│   │   └── ProfileView.swift
│   └── Components/
│       ├── TabBarView.swift
│       └── LoadingView.swift
├── Services/                                      # 网络与服务层
│   ├── NetworkService.swift                      # URLSession 封装
│   ├── AuthService.swift                         # 认证相关 API
│   ├── PlanService.swift                         # 计划相关 API
│   ├── CheckinService.swift                      # 打卡相关 API
│   ├── GroupService.swift                        # 小组相关 API
│   └── ReminderService.swift                     # 提醒相关 API
├── Utils/                                         # 工具类
│   ├── KeychainManager.swift                     # Keychain 存储 Token
│   ├── NotificationManager.swift                 # 本地通知管理
│   └── DateUtils.swift                           # 日期格式化工具
└── Resources/
    └── Assets.xcassets
```

### 3.2 MVVM 架构

```
┌──────────┐      ┌──────────────┐      ┌─────────────┐
│   View   │ ←──→ │  ViewModel   │ ────→ │   Service   │
│ (SwiftUI)│      │ (@Published) │      │ (Network)   │
└──────────┘      └──────────────┘      └─────────────┘
                         │                      │
                         ▼                      ▼
                  ┌─────────────┐       ┌─────────────┐
                  │    Model    │       │  Keychain   │
                  │  (Codable)  │       │  (Token)    │
                  └─────────────┘       └─────────────┘
```

| 层级 | 职责 |
|------|------|
| View | UI 展示，绑定 ViewModel 数据，触发用户操作 |
| ViewModel | 持有状态（@Published），调用 Service，处理业务逻辑 |
| Service | 封装网络请求，返回解码后的 Model |
| Model | Codable 数据结构，对应 API 响应 |

### 3.3 网络层设计

```swift
// NetworkService 核心设计
class NetworkService {
    static let shared = NetworkService()
    private let baseURL = "http://localhost:8080/api"

    func request<T: Codable>(
        endpoint: String,
        method: HTTPMethod,
        body: Encodable? = nil,
        requiresAuth: Bool = true
    ) async throws -> Result<T>
}
```

**特性：**
- 基于 `async/await` 的现代异步 API
- 自动从 Keychain 读取 Token 附加到 Header
- 统一解析 `Result<T>` 响应格式
- Token 过期（401）自动跳转登录页

### 3.4 本地通知设计

```swift
class NotificationManager {
    // 请求通知权限
    func requestPermission()

    // 设置每日提醒
    func scheduleDailyReminder(at time: DateComponents)

    // 取消所有提醒
    func cancelAllReminders()

    // 检查当日是否已打卡（本地判断）
    func shouldShowReminder() -> Bool
}
```

---

## 4. 部署架构（开发环境）

### 4.1 本地开发

```
┌─────────────┐      ┌──────────────────┐      ┌─────────┐
│ iOS Simulator│ ──→  │ Spring Boot      │ ──→  │ MySQL 8 │
│ or iPhone    │      │ localhost:8080   │      │ :3306   │
└─────────────┘      └──────────────────┘      └─────────┘
                                                     │
                                              ┌─────────┐
                                              │  Redis  │
                                              │  :6379  │
                                              └─────────┘
```

### 4.2 启动顺序

1. 启动 MySQL 8，创建数据库并执行 DDL
2. 启动 Redis（可选，MVP 不强依赖）
3. 启动 Spring Boot 后端（`mvn spring-boot:run`）
4. 验证 Knife4j 文档：`http://localhost:8080/doc.html`
5. 运行 iOS App（Xcode → iOS Simulator）

### 4.3 配置说明

```yaml
# application-dev.yml 关键配置
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/arch_study_checkin?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
  redis:
    host: localhost
    port: 6379

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: ASSIGN_ID

jwt:
  secret: ${JWT_SECRET}
  expiration: 604800000  # 7天（毫秒）
```

---

## 5. 关键设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 认证方案 | JWT 无状态 | 移动端友好，无需服务端 session |
| ORM | MyBatis-Plus | 减少样板代码，灵活支持复杂 SQL |
| 主键 | 雪花算法 | 分布式友好，有序递增 |
| 打卡幂等 | 数据库唯一索引 + 业务 upsert | 简单可靠，无需分布式锁 |
| iOS 网络 | URLSession + async/await | 原生方案，无三方依赖 |
| Token 存储 | Keychain | iOS 安全存储标准方案 |
| 本地通知 | UserNotifications | 系统原生，无需推送证书 |
| 密码加密 | BCrypt | Spring Security 默认，安全性高 |
| API 前缀 | /api | 简洁，RESTful 风格 |
| 逻辑删除 | MyBatis-Plus @TableLogic | 全局配置，自动追加条件 |

---

## 6. MVP 技术约束

| 约束 | 说明 |
|------|------|
| 无远程推送 | 仅 iOS 本地通知，不涉及 APNs |
| 无文件上传 | 打卡内容仅文字 |
| 无 WebSocket | 所有数据通过 HTTP 轮询获取 |
| 单机部署 | 后端单实例运行，不考虑集群 |
| Redis 可选 | 核心逻辑不依赖 Redis，纯 MySQL 保证 |
| 无消息队列 | 所有操作同步处理 |

---

## 7. 后续扩展预留

| 扩展方向 | 预留设计 |
|----------|----------|
| 远程推送 | reminder_setting 表已预留，后续接入 APNs |
| 分布式部署 | 雪花算法 ID、JWT 无状态认证已支持 |
| 缓存优化 | Redis 配置已预留，可逐步引入缓存层 |
| 第三方登录 | 用户表可扩展 oauth_provider 字段 |
| 图片上传 | 可扩展 OSS 对象存储服务 |
