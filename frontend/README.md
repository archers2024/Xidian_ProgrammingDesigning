# 前端工程说明

Vue 3 + TypeScript + Vite 的校园活动管理系统前端。后端为独立 Spring Boot 工程，前端通过同源 `/api` 访问。

## 1 环境要求

| 项目 | 版本 | 说明 |
|---|---|---|
| Node.js | ≥ 20.19（已在 24.21.0 验证） | Vite 6 的运行要求 |
| npm | 11.x | 只使用 npm，仓库中只保留 `package-lock.json` |
| 后端 | http://localhost:8080 | 由 `VITE_API_PROXY_TARGET` 指定 |

## 2 启动

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

- 开发环境默认连接 Spring Boot（`.env.development` 中 `VITE_USE_MOCK=false`）。先启动后端，再运行 `npm run dev`。演示账号 `admin / admin123`。
- 后端未启动时，可临时把 `VITE_USE_MOCK` 改为 `true`。模拟数据在内存中，刷新后恢复。模拟模式不上传文件，也不生成签到二维码。
- 手机调试：手机与电脑处于同一局域网，访问 `http://<电脑IP>:5173/events/<活动ID>`。Vite 已监听全部网卡，`/api` 代理到后端。二维码对外地址必须使用手机可访问的地址，不能是 `localhost`。

## 3 脚本

| 命令 | 作用 |
|---|---|
| `npm run dev` | 开发服务器（含 `/api` 代理） |
| `npm run type-check` | `vue-tsc` 严格类型检查 |
| `npm run build` | 类型检查 + 生产构建，输出 `dist/` |
| `npm run build:only` | 跳过类型检查直接构建 |
| `npm run preview` | 本地预览构建产物 |

## 4 目录结构

```text
frontend/src/
├── api/                http.ts（信封/CSRF/401）、csrf.ts、admin/、public/
├── types/              与接口契约一致的 TypeScript 类型
├── mocks/              同协议的模拟接口（适配器 + 演示数据）
├── layouts/            AdminLayout、PublicLayout
├── router/             路由表、守卫、RouteMeta 扩展
├── stores/             auth 会话状态
├── styles/             tokens、global、element-theme、transitions
├── components/         common/（通用）、activity/（活动表格）
├── composables/        useAsyncData（取消旧请求、错误归一化）
├── utils/              时间与格式化
└── views/admin|public  页面
```

## 5 开发约定

1. 页面不直接调用 `axios`/`fetch`，统一通过 `src/api`；模拟数据只在 `src/mocks`，不散落到页面。
2. 接口字段以 `docs/api/openapi.yaml` 为权威。字段变化时先改契约和 `types/`、`api/`，再改页面。
3. 活动展示状态固定为 `DRAFT / REGISTERING / CLOSED / ENDED`，由后端按服务器时间计算；名额已满通过 `unavailableReason=CAPACITY_FULL` 表达，不新增第五种状态。
4. 筛选条件、分页、详情页标签页写入 URL（`?keyword=&status=&page=&tab=`），返回列表时恢复。
5. 错误处理按业务码分支（`REGISTRATION_DUPLICATE`、`VERSION_CONFLICT` 等），不依赖中文提示文本。
6. 时间统一按 `Asia/Shanghai` 展示，接口使用带时区的 ISO 8601；时间边界判断以后端返回为准。
7. 页面切换只让布局内容区参与转场，导航与顶栏保持稳定；转场时长与 `prefers-reduced-motion` 见 [前端设计规范](../docs/design/frontend-design.md)。

## 6 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_APP_TITLE` | 校园活动管理 | 浏览器标题与品牌区 |
| `VITE_API_BASE_URL` | `/api/v1` | 接口基础路径 |
| `VITE_CSRF_HEADER` | `X-CSRF-TOKEN` | 与后端 Spring Security 的 CSRF 头一致 |
| `VITE_USE_MOCK` | `false`（开发） | `true` 时使用 `src/mocks` |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Vite 代理目标 |

`VITE_` 变量会进入浏览器产物，禁止写入后端密钥、数据库密码或私钥。

## 7 当前完成度

| 范围 | 状态 |
|---|---|
| 构建配置、设计变量、Element Plus 主题映射 | 已完成 |
| 路由、后台/公开布局、页面转场、鉴权守卫 | 已完成 |
| Axios 封装（响应信封、CSRF、401、取消请求） | 已完成 |
| 模拟接口与演示数据（含四种活动状态） | 已完成 |
| 登录、工作台、活动列表（筛选/分页/发布/删除）、创建与编辑、活动详情、宣传页、报名页 | 已可操作 |
| 报名名单 | 已接入活动详情 |
| 封面、附件、宣传页二维码、签到大屏与手机签到 | 已接入 |

## 8 接口约定

正式契约见 [docs/api/openapi.yaml](../docs/api/openapi.yaml)。前端成功码接受 `OK`（后端实际返回值），并兼容 `SUCCESS` 与 `0`。常见错误码包括 `VALIDATION_ERROR`、`VERSION_CONFLICT`、`REGISTRATION_DUPLICATE`、`REGISTRATION_FULL`、`REGISTRATION_CLOSED`、`ACTIVITY_UNAVAILABLE`、`CHECKIN_TOKEN_INVALID`、`CHECKIN_TOKEN_EXPIRED`、`UNAUTHENTICATED`、`CSRF_INVALID`。

## 9 构建与部署

```bash
npm run build        # dist/
```

正式环境由 Nginx 托管 `dist/`，`/api` 反向代理到 Spring Boot；前端路由回退只用于页面路径，`/api` 与不存在的静态文件不能被回退成 HTML。

## 10 已知限制

- Element Plus 采用全量 CSS（`dist` 中约 378 KB，gzip 约 52 KB），后续可按需引入样式优化首屏。
- 尚未添加单元测试与端到端测试；`frontend/tests/` 在接入测试时创建。
- 模拟模式不上传文件，也不生成签到二维码。这两项需要 `VITE_USE_MOCK=false` 且后端已启动。
