# 数据库文件说明

`campus_activity` 库（MySQL 8，`utf8mb4 / utf8mb4_general_ci`），8 张表，表结构与设计说明见 `docs/05-数据库设计.md`。

## 本机运行库的位置（重要）

本机实际运行的实例使用**工程内**的数据目录，**不是** `C:\ProgramData\MySQL` 的默认目录：

```
db\data\        ← 数据本体（约 197MB），不要删除
```

- 由工程根目录的 `启动全部.cmd` 以 `--datadir=<工程根>\db\data` 启动，监听 `127.0.0.1:3306`，账号 `root / 123456`。
- **工程路径必须是纯英文**：mysqld 在 Windows 下解析含中文的 `--datadir` 会截断路径，报 `Failed to set datadir to 'D:\Users\...\AI\'`（已实测）。当前路径 `D:\Users\15464\Desktop\AIWorkSpace\WorkBuddy\activity-system` 是纯英文，所以数据可以放在工程内；如果把 `AIWorkSpace` 改回中文名（如 `AI工作区`），MySQL 会启动失败。
- `db/` 目录下的 `.sql` 文件只是初始化 / 导出用的快照，**不是**运行时数据。
- 备份方式：停服务后直接复制整个 `db\data` 目录，或运行 `export-dump.cmd` 导出 SQL。

## 文件清单

| 文件 | 内容 | 什么时候用 |
| --- | --- | --- |
| `init.sql` | **官方初始化脚本**：建库、8 张表 DDL、活动类型与学院字典数据 | 第一次搭环境，或要把库重置为空 |
| `campus_activity_dump.sql` | **真实 mysqldump 导出**（MySQL 8.0.46，`--single-transaction --databases`），含 `CREATE DATABASE` + 8 张表 + **当前运行数据**（2 个活动、1 条报名、1 条签到、2 个账号） | 归档、迁移、**恢复现场**；clone 后想立刻看到数据就用这个 |
| `campus_activity_snapshot.sql` | 等价快照：`init.sql` 全文 + 中文注释与精简头尾，格式对齐 mysqldump | 想读一份带中文说明的自包含脚本时 |
| `demo-data.sql` | 演示数据：1 个进行中的活动、2 条报名、1 条签到 | 答辩 / 截图 / 快速看统计图表 |
| `export-dump.cmd` | 从**运行中的 MySQL** 重新导出 `campus_activity_dump.sql` | 你把库改过、要导出当前真实数据时 |

`init.sql`、`campus_activity_snapshot.sql`、`demo-data.sql` 都在一个临时 MySQL 8.0.46 实例上实测导入通过，校验结果为 8 张表、5 类活动类型、7 个学院、1 个活动、2 条报名、1 条签到。

`campus_activity_dump.sql` 由 `export-dump.cmd` 从运行库重新导出，**当前内容含 2 个活动、1 条报名、1 条签到、2 个账号**，已在临时库 `campus_activity_verify` 实测还原通过（8 张表、数据条数一致）。它是恢复现场的版本，`init.sql` 是空库起步的版本，按需选用。

> `db/data/` 是 MySQL 的二进制运行目录，已在 `.gitignore` 中排除，**不要提交**：里面有 TLS 私钥（`private_key.pem`、`ca-key.pem`）、`auto.cnf` 的 server UUID、运行中的 redo/临时表空间，且与 MySQL 版本强绑定。要带走数据请用 `campus_activity_dump.sql`。

## 导入

```bash
# 干净库（二选一，效果相同）
mysql --default-character-set=utf8mb4 -uroot -p < db/init.sql
mysql --default-character-set=utf8mb4 -uroot -p < db/campus_activity_snapshot.sql

# 再加演示数据（可选）
mysql --default-character-set=utf8mb4 -uroot -p < db/demo-data.sql
```

`--default-character-set=utf8mb4` 不能省。Windows 客户端默认 GBK，中文学院名会报 `ERROR 1366`。

导入后的干净库状态：8 张表、5 类活动类型、7 个学院、**0 活动 / 0 报名 / 0 签到**。默认超级管理员不写在 SQL 里，后端 `DataInitializer` 首次启动时检测 `t_admin` 为空后用 BCrypt 写入 `admin / admin123`。

## 重新导出真实运行库

仓库里的 `campus_activity_dump.sql` 已经是真实 mysqldump 输出（干净库状态），`campus_activity_snapshot.sql` 是同一状态的中文可读版。你改过库之后要重新导出，按下面步骤：

1. 启动 MySQL 服务（Windows 需管理员权限：`net start MySQL80`，或在「服务」里手动启动）；
2. 设置密码环境变量，运行脚本：

```cmd
set DB_PASSWORD=你的密码
cd db
export-dump.cmd
```

脚本等价于：

```bash
mysqldump --default-character-set=utf8mb4 --single-transaction --no-tablespaces \
  --databases campus_activity -uroot -p > db/campus_activity_dump.sql
```

若 `mysqldump` 不在 PATH，设置 `MYSQLDUMP_BIN` 指向 `mysqldump.exe` 完整路径。导出的文件包含 `CREATE DATABASE`，恢复到别的机器直接 `mysql < campus_activity_dump.sql` 即可。

## 注意事项

- 统计接口用到 `DATE_FORMAT` 分组，MySQL 8 默认 `ONLY_FULL_GROUP_BY` 会报 50000，处理方式见 `docs/02-运行步骤.md` 第 1.4 节。
- `demo-data.sql` 会先删除 `activity_id = 1` 的签到、报名、附件、活动，**别在有真实数据的库上跑**。
- 演示数据里的姓名与手机号是虚构的，不含任何真实个人信息。
- `deploy/demo-checkin.sql` 是另一份脚本：把已有活动改成「可签到」状态，不新增数据。
