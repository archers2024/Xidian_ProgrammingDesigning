# 部署与验收

当前没有服务器。此目录是待服务器验证的部署方案，不能视为已上线证明。无需现在购买任何外部服务。

本机先执行 Maven package 和 frontend 的 npm run check，再在工程根执行 `node scripts/package-release.mjs`。输出 `backend/target/release-时间戳/`，包含 app、web、db、deploy 和 SHA256SUMS，不包含本机数据库密码或测试会话。保存整个目录，不能只上传 jar。

## 服务器准备

需要 Linux、systemd、JDK17、MySQL8、Nginx、Python3、OpenSSL、curl，以及域名和已有可信 HTTPS 证书。具体系统安装命令待服务器系统确定。SSH 仅开放给运维来源，公网开放80/443；数据库3306和应用8080不向公网开放。内存与实例数按真实负载验收，本机诊断不能推算上线容量。

1. 本机 Git Bash：`bash deploy/upload.sh backend/target/release-时间戳 user@host 22`，脚本先校验清单，上传到 `~/activity-system-packages/`。
2. 在服务器设置 `CAMPUS_DOMAIN`、`CAMPUS_TLS_CERT`、`CAMPUS_TLS_KEY`。数据库运行口令 `CAMPUS_DB_PASSWORD` 与迁移口令 `CAMPUS_MIGRATION_PASSWORD` 必须不同，均为24–128位安全随机字符。设置至少32字节 `CAMPUS_JWT_SECRET` 和至少12位 `CAMPUS_BOOTSTRAP_PASSWORD`。通过受保护环境传入，不写聊天、Git或终端历史。
3. 已确认目标为全新安装时，root 环境执行 `bash <包目录>/deploy/install.sh <包目录>`。本地 MySQL root 需要有建库建用户权限，远程数据库需调整流程。脚本建立最小运行权限、单独迁移账号、systemd 服务、HTTPS反代，执行迁移并检测就绪。
4. 第一次使用 admin 和自设初始密码登录，必须改密；完成后从 `/etc/activity-system/runtime.env` 删除一次性 CAMPUS_BOOTSTRAP_PASSWORD。该环境文件为root600，勿传给普通账号。
5. `bash <包目录>/deploy/smoke-test.sh https://域名`，再人工验证手机定位、报名、审批、下载、403、重启、日志和备份恢复。没有可信证书不能验收真实手机准确签到。

## 升级与回退

备份数据库和附件，核查新迁移兼容性，设置仅迁移进程可读的 CAMPUS_MIGRATION_URL/USER/PASSWORD，再执行 `bash <新包目录>/deploy/release.sh <新包目录>`。校验文件后迁移、建立只读版本目录、原子切换软链接并等待就绪，失败时切回旧应用。数据库不会自动逆向迁移，故不兼容迁移必须单独安排停机和恢复方案。手动回退用 `rollback.sh`，参数见脚本。

已有旧库不运行 init.sql。先备份并核查结构，再使用 MigrationTool 的显式基线参数；校验和不匹配或失败迁移应调查处理，不能改账本伪装成功。源码的历史增量 SQL 仅供追溯，新安装执行打包版本迁移。

## 备份与运维

`backup.sh` 接收 CAMPUS_BACKUP_DIR、CAMPUS_BACKUP_PASSPHRASE、CAMPUS_BACKUP_MYSQL_DEFAULTS（root600 MySQL选项文件）。需要数据库与文件一致性时先停写，备份完成后恢复写入。输出加密备份、SHA256和HMAC；三者一起转存受保护异地目录，并另存恢复口令。不能仅凭备份文件存在认定可恢复。

`restore-drill.sh <备份路径> campus_restore_独立名称` 校验认证与清单后导入全新隔离库，保留附件供核验，不覆盖生产。演练完成后检查记录数、文件哈希、登录和业务流程，再记录真实RPO/RTO。保留周期、异地传输、定时执行和告警接收人需服务器落地时配置。日志已有滚动上限；Actuator readiness/liveness 可匿名查看状态，Prometheus 需要超管认证，监控平台尚未部署。

## 外部接入

实名认证：配置 CAMPUS_IDENTITY_SECRET、CAMPUS_IDENTITY_ISSUER、CAMPUS_IDENTITY_AUTHORIZE_URL，使用独立密钥并核对签名、audience、有效期、手机号归属。当前只通过本地签名fixture，未接通真实学校认证，不应宣称实名防代签已上线。

文件默认保存在服务器状态目录。多实例跨机器运行需共享存储；私有 HTTPS 对象网关适配器通过 campus.file.storage=http、object-endpoint、object-token 配置，网关须支持PUT/GET/DELETE。真实网关的超时、404重试和权限需单独联调。短信、统一身份、真实对象网关尚未配置。
