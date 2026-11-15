# 公网部署手册：阿里云学生机 · 2核2G · 公网 IP + HTTP

本手册对应已选定的方案。整套部署包在 `deploy/` 目录，**不需要在服务器上装 Node、Maven，也不用重新编译**——`frontend/dist` 和 `backend/target/activity-system-1.0.0.jar` 都是现成产物。

| 项 | 选定值 |
| --- | --- |
| 地域 / 访问 | 中国大陆地域，`http://<公网IP>`，**不用域名、不用备案** |
| 机器 | 2核2G / 40G 系统盘 / 1–3M 带宽 |
| 操作系统 | Ubuntu 22.04 LTS 64 位 |
| 运行环境 | Nginx（静态页 + 反向代理）、JDK 17、MySQL 8 |
| 进程守护 | systemd（`activity-system.service`） |
| 准确签到 | ❌ 不可用（HTTP 非安全上下文，浏览器不给定位权限），演示走基本签到 |

---

## 一、买服务器

1. **先做学生认证**：打开 [阿里云高校计划「云工开物」](https://university.aliyun.com/) →「我是学生」→ 实名认证 + 学信网学籍核验。认证后每 12 个月可领 **1 张 300 元无门槛券**，有效期 1 年、每日限量。
2. **进 [学生权益专区](https://university.aliyun.com/buycenter/)** 选「包年包月」里的云服务器。券只覆盖部分公共云产品，**不含域名**，通常也不能与折扣价叠加、同一商品只能抵扣一次；结算页显示的实付金额才是最终价格。第三方攻略里的「99 元学生机 / 38 元秒杀 / 算力包」价格波动大，以专区实时页面为准。
3. **配置底线**：≥ 2核2G、系统盘 ≥40G、带宽 ≥1M。**不要买 1核1G 或 2核0.5G 的「0 元机」**——MySQL 8 在这类机器上基本起不来。预算允许就上 2核4G，演示时不用精打细算。
4. **镜像选 Ubuntu 22.04 LTS 64 位**（本部署包按 apt + systemd 编写，Alibaba Cloud Linux / CentOS 命令不同）。地域选离你近的大陆节点。
5. **安全组入方向**只放行：
   - `80/tcp` → `0.0.0.0/0`（网站）
   - `22/tcp` → 建议只填自己电脑的出口 IP（SSH）
   - **不要开放 3306（MySQL）和 8080（后端）**，它们只在本机 127.0.0.1 上访问。
6. 记下控制台给的**公网 IP**。

> 关于备案：本方案用裸公网 IP 访问，不需要域名、不需要 ICP 备案。以后若换成域名并解析到大陆服务器，必须先完成 ICP 备案（通常 15–20 天），否则 80/443 会被云平台拦截。

---

## 二、从本机上传文件

在本机 **Git Bash** 里执行（Windows 10/11 自带 OpenSSH 客户端）：

```bash
cd "D:/Users/15464/Desktop/AI工作区/WorkBuddy/activity-system/deploy"
bash upload.sh root@<公网IP>
```

脚本会把 jar、`init.sql`、`dist/`、整个 `deploy/` 传到服务器 `~/activity-system-pkg/`。若 `scp` 不可用，用 WinSCP / FinalShell 手动传成同样结构：

```
~/activity-system-pkg/
├── activity-system-1.0.0.jar      ← backend/target/
├── init.sql                       ← db/
├── dist/                          ← frontend/dist/ 里的全部内容
└── deploy/                        ← 本目录全部文件
```

---

## 三、服务器一键初始化

SSH 登录服务器后：

```bash
cd ~/activity-system-pkg/deploy
sudo bash setup.sh <公网IP>
```

`setup.sh` 会依次完成 10 步：检查上传文件 → 装 JDK 17 / MySQL 8 / Nginx → 设上海时区 + 2G swap → 写 MySQL 低内存配置 → 幂等导入 `init.sql` → 创建随机密码的数据库账号 `campus` → 部署程序到 `/opt/activity-system` 并生成外置配置 → 注册 systemd 服务 → 配 Nginx（含 gzip、history 回退、反代）→ 等待后端就绪。

首次约 5–10 分钟。**执行完会打印数据库密码，记下来**。脚本可重复执行，检测到已有表时会跳过 `init.sql`（那个脚本含 `DROP TABLE IF EXISTS`，重复导入会清空数据）。

关键配置都已自动改成公网可用值：

| 配置 | 开发默认值 | 生产值 |
| --- | --- | --- |
| `campus.mobile.base-url` | `http://localhost:5173/m` | `http://<公网IP>/m` |
| `campus.file.upload-dir` | `./uploads` | `/opt/activity-system/uploads` |
| `campus.jwt.secret` | 明文固定串 | 随机 32 字节 |
| 数据库账号 | `root/root` | `campus/<随机>` |
| `campus.signup.ip-limit-per-minute` | 10 | 100（配合真实 IP 还原） |
| JVM 堆 | 不限制 | `-Xmx512m` |

`application-prod.yml` 里还启用了 Tomcat `RemoteIpValve`：Nginx 反代后 `request.getRemoteAddr()` 本来会全部变成 `127.0.0.1`，导致报名的「同 IP 每分钟 10 次」限流变成全校共用一个计数。配合 `X-Forwarded-For` 才能按真实客户端 IP 限流。

---

## 四、验证

```bash
bash ~/activity-system-pkg/deploy/smoke-test.sh <公网IP>
```

会检查首页、深链刷新、移动端页面、静态资源 gzip、字典接口、登录接口。浏览器打开 `http://<公网IP>`，用 **admin / admin123** 登录，**第一件事就是去「系统管理 → 账号管理」改掉默认密码**（改完之后冒烟脚本第 6 项会提示 WARN，属正常）。

运维常用命令：

| 目的 | 命令 |
| --- | --- |
| 后端状态 / 日志 | `systemctl status activity-system` / `journalctl -u activity-system -f` |
| 重启后端（改配置后） | `sudo systemctl restart activity-system` |
| 看应用日志 | `tail -f /opt/activity-system/logs/backend.log` |
| 检查 Nginx 配置 | `sudo nginx -t` → `sudo systemctl reload nginx` |
| 改外置配置 | `sudo vim /opt/activity-system/config/application-prod.yml`，然后重启后端 |
| 备份数据库 | `mysqldump campus_activity > ~/backup-$(date +%F).sql` |
| 更新后端 jar | 覆盖 `/opt/activity-system/app/activity-system-1.0.0.jar` 后 `sudo systemctl restart activity-system` |

---

## 五、演示主链路（HTTP 版，与本地开发有差异）

1. 登录后台 → 立即改密码。
2. **活动列表 → 新建活动**：名称/类型/地点/简介必填；名额 1–5000；时间必须满足 **报名截止 < 开始 < 结束**，且报名截止设在未来（否则建完立刻变成 CLOSED，报不了名）。
3. 保存后是 `DRAFT` → 详情页点「发布」→ 变 `OPEN`。
4. 详情页「查看宣传页二维码」→ 手机扫码进 `/m/promo/{guid}` →「立即报名」→ 姓名 2–20 字、手机号 `1[3-9]` 开头 11 位、选学院 → 报名成功。同一手机号重复报名会被唯一索引拦下。
5. 后台「报名名单」看脱敏手机号、导出 Excel。
6. **报名做完后**，在服务器上开启签到（把 `@aid` 改成详情页 URL 里的活动 ID）：
   ```bash
   sed -i 's/SET @aid = 1;/SET @aid = <实际ID>;/' ~/activity-system-pkg/deploy/demo-checkin.sql
   mysql campus_activity < ~/activity-system-pkg/deploy/demo-checkin.sql
   ```
   这条 SQL 同时做两件事：把写死为 0 的 `checkin_enabled` 打开，并把时间字段挪成「已开始未结束」，让状态机自然算出 `ONGOING`。**不能只手工改 status**——`StatusRefreshJob` 每 60 秒会按时间字段重算并覆盖回去。
7. 刷新详情页，状态显示「进行中」。
8. **签到（关键差异）**：手机直接打开
   ```
   http://<公网IP>/m/checkin/<活动ID>
   ```
   **不要带 `?code=`**。输入报名手机号 → 签到成功，方式显示「基本签到」。

   为什么不能用「打开签到大屏 → 扫动态码」：`CheckinView.vue` 只要 URL 里带 `code`，就强制调用 `navigator.geolocation` 取定位；HTTP 公网 IP 不是安全上下文，Chrome/微信直接拒绝授权，代码会报「需开启定位权限才能完成准确签到」并且**不会降级到基本签到**。想演示动态码 + 地理围栏的准确签到，必须上 HTTPS（见第九节）。
9. 后台「签到记录」→ 导出；「数据统计」看活动数、报名人次、签到率、趋势图。

**二维码为什么能自动指向公网**：后台详情页和签到大屏的二维码用 `location.origin` 拼，只要你是用 `http://<公网IP>` 打开的后台就正确；宣传页里那个小二维码由后端 `campus.mobile.base-url` 拼，`setup.sh` 已经写成公网地址。别用 `localhost` 打开后台再截图二维码。

---

## 六、公网暴露前的安全检查

- ✅ 改掉 `admin/admin123`（账号管理页）。
- ✅ 数据库账号 `campus` 只允许 `127.0.0.1` 连接，密码随机；3306 不出安全组。
- ✅ JWT 密钥随机生成，配置文件 `chmod 600`。
- ⚠️ 后端 `WebConfig` 里 CORS 仍是 `allowedOriginPatterns("*")`（源码未改），同源部署下浏览器不会用到跨域，演示可接受；若正式对外运营应收敛到具体域名。
- ⚠️ 上传文件走 `/api/v1/files/` 由后端映射，Nginx 没有直接暴露 `uploads` 目录。
- ⚠️ `init.sql` 含 `DROP TABLE IF EXISTS`，**任何情况下都不要在有数据的库上重跑**。

---

## 七、排错

| 现象 | 处理 |
| --- | --- |
| 浏览器打不开 | 阿里云安全组没放 80；`systemctl status nginx`；镜像预装了宝塔/Apache 占了 80 |
| 页面能开、接口 502 | 后端没起：`systemctl status activity-system` + `journalctl -u activity-system -n 100` |
| `Communications link failure` | MySQL 没起：`sudo systemctl restart mysql` |
| `Access denied for user 'campus'` | 重跑 `setup.sh` 会重建账号与配置；或手动 `ALTER USER` 后同步改 `application-prod.yml` |
| 刷新 `/activity/1` 报 404 | 确认 `/etc/nginx/sites-enabled/default` 已删除、`nginx -t` 通过 |
| 二维码扫出来是 localhost | 检查 `/opt/activity-system/config/application-prod.yml` 的 `campus.mobile.base-url` |
| 报名提示「操作过于频繁」 | `X-Forwarded-For` 没传或 `RemoteIpValve` 未生效；检查 Nginx 配置与后端日志里的来源 IP |
| 「该活动未开启签到」/「当前不在签到时间范围内」 | 执行第五节第 6 步的 `demo-checkin.sql` |
| 「需开启定位权限才能完成准确签到」 | HTTP 下的正常现象，改用不带 `?code=` 的基本签到链接 |
| 「活动已发布，请勿重复操作」 | 状态已不是 DRAFT，别再点发布 |
| 手机扫二维码打不开 | 确认用的是 `http://<公网IP>` 而不是 localhost，且手机与服务器网络互通 |

---

## 八、已知源码限制（本次部署未修改任何 Java / Vue 源码）

| 限制 | 影响 | 本部署包的处理 |
| --- | --- | --- |
| `checkin_enabled` 建活动时写死 0，且没有任何接口能开启 | 不处理则签到永远失败 | `demo-checkin.sql` 从数据库打开 |
| 报名只放行 `OPEN`、签到只放行 `ONGOING/CLOSED` | 两者互斥，无法既报名又签到 | 按「先报名 → 后执行 SQL」的顺序演示 |
| `refreshStatus` 每 60 秒按时间重算状态 | 手工 `UPDATE status` 会被覆盖 | SQL 改时间字段而非只改状态 |
| `CheckinService.statusByActivity` 用 `checkinId` 当 `regId` 做 map key | 报名名单里签到状态可能显示不准 | 签到记录页、导出、统计均正常；改需重新打包 |
| `LoginInterceptor` 注释里的开放路径与实际 URI 不一致 | 仅注释问题 | 不影响运行 |
| 原 README 注明「未做运行时联调」 | 首次完整上线可能出现接口细节问题 | 用 `smoke-test.sh` 定位，把日志发回排查 |

---

## 九、以后想升级到 HTTPS（解锁准确签到）

准确签到依赖浏览器定位，只在 **HTTPS 安全上下文**（或 localhost）下可用。两条路：

- **大陆地域 + 域名**：买域名 → 完成 ICP 备案（15–20 天）→ 申请免费 SSL 证书 → Nginx 增加 443 `server` 块并把 80 重定向 → 把 `campus.mobile.base-url` 改成 `https://域名/m` → 重启后端。备案期间可继续用公网 IP 的 HTTP 地址演示。
- **免备案地域**：把服务器换到中国香港/海外节点 + 域名 + Let's Encrypt，不用等备案，但国内访问延迟偏高、学生优惠可选机型更少。

---

## 十、deploy/ 文件清单

| 文件 | 用途 |
| --- | --- |
| `README.md` | 本手册 |
| `upload.sh` | 本机（Git Bash）上传到服务器 |
| `setup.sh` | 服务器一键初始化，10 步完成部署 |
| `nginx.conf` | 站点配置：gzip、`/api/v1/` 反代、history 回退、25MB 上传 |
| `application-prod.yml` | 外置生产配置模板（占位符由 setup.sh 替换） |
| `activity-system.service` | systemd unit，含 2G 内存的 JVM 堆限制 |
| `mysql-lowmem.cnf` | MySQL 8 低内存参数 |
| `demo-checkin.sql` | 演示前开启签到并推进状态 |
| `smoke-test.sh` | 部署后冒烟测试 |
