# 高校学生活动全流程管理系统

校园活动从创建、发布、报名到现场签到的一套系统。前端是 Vue 3 + TypeScript，后端是 Spring Boot 3 + MyBatis。浏览器只访问前端开发服务器，接口由 Vite 代理到后端，会话 Cookie 保持同源。

## 环境

| 项目 | 要求 |
|---|---|
| JDK | 17。不要用 JDK 25 启动 Spring Boot 3.4 |
| Maven | 后端自带包装器 `backend/mvnw.cmd`（Maven 3.9.11） |
| Node.js | 20.19 及以上 |
| 数据库 | 本机用 H2 文件库，数据在 `runtime/data/`。达梦 DM8 建表脚本在 `database/dm/schema.sql` |

`runtime/` 已加入忽略列表，不要提交上传文件和本地数据库。

## 启动

先启动后端，工作目录必须是 `backend/`，这样上传目录和数据库会落到仓库的 `runtime/`。

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
cd backend
.\mvnw.cmd spring-boot:run
```

服务监听 `http://localhost:8080`。若本机 JDK 17 不在上面的路径，把 `JAVA_HOME` 改成实际安装目录。

再启动前端：

```powershell
cd frontend
npm install
npm run dev
```

打开 `http://localhost:5173`。演示账号 `admin` / `admin123`。

首次启动且管理员表为空时，会写入示例活动、类型、学院和一条可签到的讲座。之后重启会保留已有数据。

## 主要路径

| 页面 | 地址 |
|---|---|
| 登录与工作台 | http://localhost:5173/login |
| 活动列表 | http://localhost:5173/admin/activities |
| 公开宣传页 | http://localhost:5173/events/act-ai-lecture |
| 匿名报名 | http://localhost:5173/events/act-ai-lecture/register |
| 签到大屏 | 活动详情的签到页签中打开 |

接口前缀是 `/api/v1`。契约见 `docs/api/openapi.yaml`。管理接口需要登录，写操作还要带 `X-CSRF-TOKEN`（先 `GET /api/v1/auth/csrf`）。

## 行为说明

- 活动展示状态只有 `DRAFT`、`REGISTERING`、`CLOSED`、`ENDED`，由服务器按 Asia/Shanghai 当前时间计算。名额已满是 `unavailableReason=CAPACITY_FULL`，不是第五种状态。
- 报名以活动行锁和 `(activity_id, phone)` 唯一约束控制名额与重复报名。
- 动态签到码每 30 秒更换，使用 HMAC-SHA256。过期或被改过的码会拒绝。重复签到不新增记录。公开页面不签发新码。
- 封面限 PNG、JPEG、GIF、WEBP，不超过 5MB。附件限 PDF、DOCX、PPTX，不超过 20MB。

## 测试

```powershell
cd backend
mvn test
cd ../frontend
npm run type-check
```

后端测试使用内存 H2，覆盖登录、发布、报名去重、最后一名额并发，以及动态签到码的篡改和过期。
