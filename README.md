# 高校学生活动全流程管理系统

前后端分离的课程实训项目：PC 管理后台负责活动发布、报名名单、签到记录与数据统计；移动端 H5 负责宣传页、在线报名、扫码签到与签到大屏。全流程闭环为「建活动 → 发布 → 扫码看宣传页 → 报名 → 扫码签到 → 导出名单 / 查看统计」。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 3.2.5、Java 17（可在 JDK 25 运行）、MyBatis-Plus、MySQL 8、JJWT、Spring Security Crypto（BCrypt）、ZXing、Apache POI、Hutool |
| 前端 | Vue 3.4、Vite 5、Vue Router 4、Pinia、Element Plus、ECharts、axios、qrcode |
| 部署 | Nginx 反向代理 + systemd + 可执行 jar，静态资源由 Vite 构建 |
| 数据库 | 8 张表，字典表独立，满足第三范式；`db/init.sql` 一次建库建表并灌入字典数据 |

## 仓库目录

```
.
├── README.md                    本文件
├── 首次安装.cmd                 编译后端 + 安装前端依赖（英文名 install.cmd）
├── 初始化数据库.cmd             没有 MySQL 服务时建工程内实例并导入快照（init-db.cmd）
├── 启动全部.cmd                 一键启动：MySQL → 后端 → 前端（start-all.cmd）
├── 停止全部.cmd                 一键停止（stop-all.cmd）
├── scripts/
│   ├── install-all.ps1          首次安装逻辑，自动探测 JDK 与 Node
│   ├── init-db.ps1              初始化 db\data、设置 root 密码、导入 dump
│   ├── start-all.ps1            启动逻辑，自动探测 MySQL 与 JDK
│   └── stop-all.ps1             停止逻辑，MySQL 走 mysqladmin 优雅关闭
├── db/                          数据库交付物
│   ├── init.sql                 建库 + 8 张表 + 字典数据
│   ├── campus_activity_dump.sql 真实 mysqldump 导出的完整库（含 2 活动 / 1 报名 / 1 签到）
│   ├── campus_activity_snapshot.sql  等价快照（中文注释版）
│   ├── demo-data.sql            演示数据（活动/报名/签到）
│   ├── export-dump.cmd          重新导出运行库的脚本
│   └── README.md                数据库文件说明
├── backend/                     Spring Boot 工程
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd          Maven Wrapper：不装 Maven 也能构建
│   ├── src/main/java/com/campus/activity/
│   │   ├── common/              统一响应 R、业务异常、全局异常处理
│   │   ├── config/              CORS、拦截器注册、分页插件、定时任务、默认账号
│   │   ├── security/            JWT、登录拦截器、管理员上下文
│   │   ├── util/                二维码、Excel、地理围栏
│   │   └── module/              system / activity / signup / checkin 四个业务域
│   ├── src/main/resources/application.yml
│   └── start-backend.cmd        转发到根目录 start-all.cmd
├── frontend/                    Vue 3 工程（PC 后台 + H5 同工程）
│   ├── src/api/                 axios 实例与接口封装
│   ├── src/router/              后台 8 路由 + H5 4 路由
│   ├── src/views/               PC 页面
│   ├── src/mobile/              H5 页面
│   ├── vite.config.js           开发代理（剥离 /api/v1）
│   └── start-frontend.cmd       本机 Windows 启动示例
├── deploy/                      服务器部署包（Nginx、systemd、生产配置、安装与冒烟脚本）
└── docs/                        实现步骤、运行步骤、接口清单、设计文档、他人运行指南
```

## 快速开始

需要：**JDK 17+**、**MySQL 8.0.x**、**Node.js 18+**。Maven 不用装，仓库自带 `mvnw`。详细说明与排错表见 **`docs/06-他人运行指南.md`**。

> 整个项目路径必须是纯英文。mysqld 在 Windows 下解析含中文的 `--datadir` 会截断路径并报 `Failed to set datadir (OS errno: 2)`。

### 三步跑起来

**第 1 步 · 首次安装**（只跑一次）

双击 `首次安装.cmd`（`install.cmd`）：自动探测 JDK → 编译后端 jar → 安装前端依赖。首次约 5–10 分钟，之后跳过。

仓库不提交 `backend/target` 与 `frontend/node_modules`，这一步不能省。

**第 2 步 · 准备数据库**（二选一）

- **已有 MySQL 服务**：确认服务在跑，然后导入快照，并把 `scripts/start-all.ps1` 里的 `$DbPass` 改成你自己的 root 密码。
  ```cmd
  net start MySQL80
  mysql --default-character-set=utf8mb4 -uroot -p < db\campus_activity_dump.sql
  ```
- **没有 MySQL 服务**：双击 `初始化数据库.cmd`（`init-db.cmd`），在 `db\data` 建独立实例、设 root 密码 `123456`、自动导入快照，约 30 秒。

**第 3 步 · 启动**

双击 `启动全部.cmd`（`start-all.cmd`），依次拉起 MySQL → 后端 → 前端，自动跳过已在运行的组件，末尾打印访问地址。停止用 `停止全部.cmd`。

- 脚本自动探测 MySQL / JDK 安装位置，换机器一般不用改配置；探测失败才需要填 `scripts/start-all.ps1` 顶部的 `$MySQLBin` / `$JavaExe`。
- 数据目录固定为 `<工程根>\db\data`，跟着工程走。
- 会自动重设统计接口依赖的 `sql_mode`，并自动取默认路由网卡的 IP 拼手机端地址。

### 手动启动

```bash
# 1. 建库建表与字典数据（必须指定 utf8mb4，否则中文数据报 ERROR 1366）
mysql --default-character-set=utf8mb4 -uroot -p < db/init.sql
# 想立刻看到真实数据，改用完整快照（含 2 个活动、1 条报名、1 条签到）
# mysql --default-character-set=utf8mb4 -uroot -p < db/campus_activity_dump.sql
# 或导入 db/demo-data.sql 得到 1 个活动、2 条报名、1 条签到

# 2. 后端，修改 application.yml 中的数据库账号密码后执行
cd backend
mvnw.cmd -DskipTests package
java -jar target/activity-system-1.0.0.jar

# 3. 前端
cd ../frontend
npm ci
npm run dev -- --host 0.0.0.0 --port 5173
```

浏览器打开 `http://localhost:5173`，默认超级管理员 **admin / admin123**（首次启动时由 `DataInitializer` 用 BCrypt 写入，SQL 脚本里没有明文口令）。

完整步骤见 `docs/06-他人运行指南.md` 与 `docs/02-运行步骤.md`，从零到验收的实现过程见 `docs/01-实现步骤.md`，服务器部署见 `docs/03-服务器部署.md`。

## 关键约定

- 前端所有请求以 `/api/v1` 开头；Vite 代理与 Nginx 会剥离该前缀，后端以**根路径**部署（`context-path: /`）。改这个约定必须同时改 `frontend/vite.config.js`、`deploy/nginx.conf` 和 `application.yml`，否则鉴权与 404 会一起出问题。
- 登录接口返回 JWT，后续请求放在 `Authorization: Bearer <token>`；导出接口是浏览器直链，token 走 query 参数。
- MySQL 建议关闭 `ONLY_FULL_GROUP_BY`（统计接口用到 `DATE_FORMAT` 分组）；`docs/02-运行步骤.md` 给出了不改全局配置的替代办法。
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
| `docs/04-接口清单.md` | 28 个 REST 接口与前端调用位置 |
| `docs/05-数据库设计.md` | 8 张表、字段、索引与约束 |
| `docs/06-他人运行指南.md` | **别人拿到项目怎么跑**：环境检查、三步启动、常见错误对照表 |
| `db/README.md` | 数据库文件清单、导入方式、真实 dump 导出方法 |
| `docs/需求规格说明书.docx` | 原始需求 |
| `docs/软件设计报告.docx` | 依据源码反推的软件设计报告 |
| `docs/设计思路报告.docx` | 未依赖源码、从需求推演的设计思路报告 |

## 已知限制

- 未接入 Redis，登录态使用 JWT + `token_version`：停用账号、重置密码、调整角色会立即使旧令牌失效；退出仍只在前端丢弃 token。设备限频与报名 IP 限流都在单机内存计数。
- 文件上传落本地磁盘，未接对象存储；`uploads/` 不入库。
- 未做单元测试与自动化运行时联调，验收靠 `deploy/smoke-test.sh` 与手工主链路。
- `start-backend.cmd` 已改为转发到根目录的 `start-all.cmd`；需要单独控制后端时，按 `docs/02-运行步骤.md` 直接执行 `java -jar`。
- `db/data/`（MySQL 二进制数据目录，约 197MB）已在 `.gitignore` 中排除，仓库只保存 SQL 形态的数据库。它包含 TLS 私钥与运行中的 redo/临时表空间，且与 MySQL 版本强绑定，不适合版本化；要迁移数据请用 `db/campus_activity_dump.sql`。
- 项目路径必须是纯英文。`--datadir` 含中文时 mysqld 会截断路径并启动失败，这是 MySQL 在 Windows 上的限制，脚本无法绕过。
