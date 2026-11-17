# 校园协同与活动管理平台

2026-11-17：已按题目二修正所有状态编辑、发布时间展示及删除语义，并修复编辑页加载覆盖输入。最新结果见 [题目二验收报告](docs/testing/TOPIC2-ACCEPTANCE-20261117.md)。

现支持多组织活动空间、组织管理员权限隔离、工作台、平台操作审计，以及原有活动发布、报名、签到和统计流程。普通管理员管理所属组织；超级管理员管理全平台。

已完成多组织隔离、审批快照、Cookie 会话、共享限流、版本冲突保护、分页导出、文件权限与回收、健康探针和可校验部署包。最新验收口径以 [64项闭环与验收记录](docs/testing/OPTIMIZATION-CLOSURE-20261116.md) 为准。没有服务器，生产上线尚未验收。

前后端分离的课程实训项目：PC 管理后台负责活动发布、报名名单、签到记录与数据统计；移动端 H5 负责宣传页、在线报名、扫码签到与签到大屏。全流程闭环为「建活动 → 发布 → 扫码看宣传页 → 报名 → 扫码签到 → 导出名单 / 查看统计」。

## 本轮结构整理

前端统一导航、线性图标、工作台、数据卡片与列表风格，保留真实业务数据。后端将组织管理和审计从控制器下沉到服务；活动与审批使用请求 DTO、领域规则和共享组织访问边界。事务方法保留活动行锁；组织状态与策略按列更新。

维护约定见 [架构与维护约定](docs/06-架构与维护约定.md)。本地执行 `mvn -f backend/pom.xml test` 可运行不依赖数据库的单元测试；`.github/workflows/ci.yml` 提供后续 GitHub CI 工作流。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 3.2.5、Java 17（可在 JDK 25 运行）、MyBatis-Plus、MySQL 8、JJWT、Spring Security Crypto（BCrypt）、ZXing、Apache POI、Hutool |
| 前端 | Vue 3.4、Vite 5、Vue Router 4、Pinia、Element Plus、ECharts、axios、qrcode |
| 部署 | Nginx 反向代理 + systemd + 可执行 jar，静态资源由 Vite 构建 |
| 数据库 | 版本化业务表，字典表独立，满足第三范式；`db/init.sql` 一次建库建表并灌入字典数据 |

## 仓库目录

```
.
├── README.md                    本文件
├── db/init.sql                  建库 + 业务表 + 字典数据
├── backend/                     Spring Boot 工程
│   ├── pom.xml
│   ├── src/main/java/com/campus/activity/
│   │   ├── common/              统一响应 R、业务异常、全局异常处理
│   │   ├── config/              CORS、拦截器注册、分页插件、定时任务、首启账号
│   │   ├── security/            JWT、登录拦截器、管理员上下文
│   │   ├── util/                二维码、Excel、地理围栏
│   │   └── module/              system / activity / signup / checkin 四个业务域
│   ├── src/main/resources/application.yml
│   └── start-backend.cmd        本机 Windows 一键启动示例
├── frontend/                    Vue 3 工程（PC 后台 + H5 同工程）
│   ├── src/api/                 axios 实例与接口封装
│   ├── src/router/              角色路由及移动端页面
│   ├── src/views/               PC 页面
│   ├── src/mobile/              H5 页面
│   ├── vite.config.js           开发代理（剥离 /api/v1）
│   └── start-frontend.cmd       本机 Windows 一键启动示例
├── deploy/                      服务器部署包（Nginx、systemd、生产配置、安装与冒烟脚本）
└── docs/                        实现步骤、运行步骤、接口清单、设计文档
```

## 快速开始

仓库中旧版 `start-all.cmd` / `init-db.cmd` 及其 PowerShell 脚本基于旧数据库快照，尚未适配本次版本迁移和外置凭据。当前版本请按本页“快速开始”配置数据库、执行 MigrationTool 后运行，勿直接重复导入历史快照。


需要：JDK 17+、Maven 3.8+、MySQL 8、Node.js 18+（推荐 20）。

首次安装：先 `mvn -f backend/pom.xml package`。配置 `CAMPUS_MIGRATION_URL`、`CAMPUS_MIGRATION_USER`、`CAMPUS_MIGRATION_PASSWORD`，运行版本迁移：

```bash
java -Dloader.main=com.campus.activity.tools.MigrationTool -cp backend/target/activity-system-1.0.0.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

应用使用独立的 DML 数据库账号，通过 `CAMPUS_DB_URL`、`CAMPUS_DB_USER`、`CAMPUS_DB_PASSWORD` 注入。全新数据库还须设置至少12位 `CAMPUS_BOOTSTRAP_PASSWORD`；首个账号为 admin，首次登录必须改密。已有账号不会重置。已有旧库需要备份、核对结构再显式基线，不能直接重复运行历史迁移或初始化 SQL。

IDEA 打开 `backend/pom.xml`，选择 JDK17、等待 Maven 导入，运行 `com.campus.activity.ActivityApplication`，工作目录设为 `backend`。本机已有数据库配置保存在被忽略的 `backend/config/local.properties`。另开终端执行 `cd frontend`、`npm ci`、`npm run dev`，访问 `http://localhost:5173`。

服务器部署见 [部署手册](deploy/README.md)。旧文档保留历史实现记录；涉及口令、迁移、导出和部署步骤时，以本页与新部署手册为准。

## 关键约定

- 前端所有请求以 `/api/v1` 开头；Vite 代理与 Nginx 会剥离该前缀，后端以**根路径**部署（`context-path: /`）。改这个约定必须同时改 `frontend/vite.config.js`、`deploy/nginx.conf` 和 `application.yml`，否则鉴权与 404 会一起出问题。
- 浏览器使用 HttpOnly 会话 Cookie，写操作同时校验 CSRF；API 客户端可用 Bearer。导出通过鉴权请求下载，URL 不接受令牌。
- 保留 MySQL 严格模式；统计分组表达式已保持一致。
- 准确签到依赖浏览器地理位置，必须用 HTTPS 或 `localhost`；纯 HTTP 局域网访问只能走基本签到。

## 已修复的联调问题

仓库里的源码已包含以下修复，细节与复现过程记录在 `docs/01-实现步骤.md`：

1. 登录拦截器直接读 `getRequestURI()` 判断 `/admin/`，在带 `context-path` 时鉴权全失效 → 改为剥离 context-path 与网关前缀后再判断。
2. 活动创建接口把 `yyyy-MM-dd HH:mm:ss` 强行替换成 `T` 再 `LocalDateTime.parse`，空格格式必然抛异常 → 改为按格式器解析，同时兼容 ISO 与 `datetime-local`。
3. JDBC URL 写 `characterEncoding=utf8mb4` → 启动报 `Unsupported character encoding`，改为 `UTF-8`。
4. 统计 SQL `GROUP BY DATE(...)` 与 `SELECT DATE_FORMAT(...)` 不一致，在 `ONLY_FULL_GROUP_BY` 下报 1055/50000 → 分组表达式改为一致。
5. 单类重编译时必须带 `-parameters`，否则 `@RequestParam` 参数名丢失，列表接口返回 `data: null`。

## 文档索引

| 文件 | 内容 |
| --- | --- |
| `docs/01-实现步骤.md` | 从需求拆解到验收的完整实现过程、关键算法、踩坑记录 |
| `docs/02-运行步骤.md` | 本机运行与演示的超详细步骤、排错表 |
| `docs/03-服务器部署.md` | 阿里云/学生机部署流程与配置说明 |
| `docs/04-接口清单.md` | 27 个 REST 接口与前端调用位置 |
| `docs/05-数据库设计.md` | 版本化业务表、字段、索引与约束 |
| `docs/需求规格说明书.docx` | 原始需求 |
| `docs/软件设计报告.docx` | 依据源码反推的软件设计报告 |
| `docs/设计思路报告.docx` | 未依赖源码、从需求推演的设计思路报告 |

## 验证与边界

- `mvn -f backend/pom.xml test`；`npm --prefix frontend run check`。
- MySQL 集成验收：`node scripts/start-isolated.mjs` → `node scripts/run-isolated.mjs` → 在 frontend 执行 `npm run test:e2e` → `node scripts/stop-isolated.mjs`。只写入新建的 campus_test_* 库，凭据在忽略的 target 中，结果按时间保留。
- `node scripts/run-isolated.mjs cluster restore` 为本机双实例诊断及小样本恢复演练；百万行查询计划诊断使用 `scale`，不代表生产容量。
- 实名校验已支持签名认证凭证，远端文件已支持私有 HTTPS 网关适配器；尚未接入真实校园身份提供方、短信或对象网关。设备标识只能辅助风控。
- 尚无服务器，HTTPS、systemd、发布回滚、真实手机定位和远端备份恢复须在目标环境验收。CI 工作流已编写，未在远端执行。
