#!/usr/bin/env bash
# 高校学生活动全流程管理系统 - 服务器一键初始化
# 目标系统: Ubuntu 22.04 LTS，2核2G，大陆公网 IP + HTTP
# 用法: sudo bash setup.sh <公网IP> [数据库密码]
# 示例: sudo bash setup.sh 47.98.123.45
# 可重复执行；检测到已有数据表时会跳过 init.sql，不会清空数据。

set -euo pipefail

PUBLIC_IP="${1:-}"
if [[ -z "$PUBLIC_IP" ]]; then
  echo "用法: sudo bash setup.sh <公网IP> [数据库密码]"
  exit 1
fi
if [[ $EUID -ne 0 ]]; then
  echo "请用 root 执行: sudo bash setup.sh $PUBLIC_IP"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PKG_DIR="$(dirname "$SCRIPT_DIR")"
APP_ROOT=/opt/activity-system

echo "==> [1/10] 检查上传文件"
for f in "$PKG_DIR/activity-system-1.0.0.jar" "$PKG_DIR/init.sql" "$PKG_DIR/dist/index.html"; do
  if [[ ! -f "$f" ]]; then
    echo "缺少文件: $f"
    echo "请先在本机执行 deploy/upload.sh 上传完整安装包"
    exit 1
  fi
done

echo "==> [2/10] 安装依赖 openjdk-17 / mysql-server / nginx"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y --no-install-recommends \
  openjdk-17-jdk-headless mysql-server nginx curl openssl ca-certificates

echo "==> [3/10] 设置时区与 2G swap"
timedatectl set-timezone Asia/Shanghai 2>/dev/null || true
if [[ -z "$(swapon --show --noheadings)" ]]; then
  if [[ ! -f /swapfile ]]; then fallocate -l 2G /swapfile; fi
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "已创建 2G swap"
else
  echo "已存在 swap，跳过"
fi

echo "==> [4/10] MySQL 8 低内存配置"
if ! grep -q 'includedir /etc/mysql/conf.d' /etc/mysql/my.cnf 2>/dev/null; then
  echo '!includedir /etc/mysql/conf.d/' >> /etc/mysql/my.cnf
fi
cp "$SCRIPT_DIR/mysql-lowmem.cnf" /etc/mysql/conf.d/99-activity-lowmem.cnf
systemctl enable --now mysql
systemctl restart mysql

echo "==> [5/10] 建库并导入 init.sql（幂等）"
TABLE_COUNT="$(mysql -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='campus_activity'" 2>/dev/null || echo 0)"
if [[ "$TABLE_COUNT" -gt 0 ]]; then
  echo "检测到 campus_activity 已有 ${TABLE_COUNT} 张表，跳过 init.sql（避免 DROP TABLE 清空数据）"
else
  mysql < "$PKG_DIR/init.sql"
  echo "init.sql 导入完成（8 张表 + 字典数据）"
fi

echo "==> [6/10] 创建数据库专用账号"
DB_PASSWORD="${2:-$(openssl rand -hex 12)}"
JWT_SECRET="$(openssl rand -hex 32)"
mysql <<SQL
CREATE USER IF NOT EXISTS 'campus'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER 'campus'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON campus_activity.* TO 'campus'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

echo "==> [7/10] 部署程序文件到 $APP_ROOT"
if ! id -u activity >/dev/null 2>&1; then
  useradd --system --no-create-home --shell /usr/sbin/nologin activity
fi
install -d -o activity -g activity "$APP_ROOT"/{app,config,web,uploads,logs}
install -m 644 "$PKG_DIR/activity-system-1.0.0.jar" "$APP_ROOT/app/activity-system-1.0.0.jar"
cp -r "$PKG_DIR/dist/." "$APP_ROOT/web/"
sed -e "s|__PUBLIC_IP__|$PUBLIC_IP|g" \
    -e "s|__DB_PASSWORD__|$DB_PASSWORD|g" \
    -e "s|__JWT_SECRET__|$JWT_SECRET|g" \
    "$SCRIPT_DIR/application-prod.yml" > "$APP_ROOT/config/application-prod.yml"
chown -R activity:activity "$APP_ROOT"
chmod 600 "$APP_ROOT/config/application-prod.yml"

echo "==> [8/10] 注册并启动 systemd 服务"
JAVA_BIN="$(readlink -f "$(command -v java)")"
sed "s|__JAVA_BIN__|$JAVA_BIN|g" "$SCRIPT_DIR/activity-system.service" \
  > /etc/systemd/system/activity-system.service
systemctl daemon-reload
systemctl enable activity-system
systemctl restart activity-system

echo "==> [9/10] 配置 Nginx"
cp "$SCRIPT_DIR/nginx.conf" /etc/nginx/sites-available/activity-system
ln -sfn /etc/nginx/sites-available/activity-system /etc/nginx/sites-enabled/activity-system
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl enable --now nginx
systemctl reload nginx

echo "==> [10/10] 等待后端就绪"
READY=0
for _ in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1/api/v1/dict/activity-type" >/dev/null 2>&1; then READY=1; break; fi
  sleep 2
done
if [[ "$READY" -ne 1 ]]; then
  echo "后端未在 60 秒内就绪，最近日志："
  journalctl -u activity-system -n 60 --no-pager
  tail -n 60 "$APP_ROOT/logs/backend.log" 2>/dev/null || true
  exit 1
fi

echo
echo "==================== 部署完成 ===================="
echo "访问地址   : http://$PUBLIC_IP"
echo "管理员账号 : admin / admin123   （登录后请立即修改密码）"
echo "数据库账号 : campus / $DB_PASSWORD  （只监听本机，未对公网开放）"
echo "配置文件   : $APP_ROOT/config/application-prod.yml"
echo "后端日志   : journalctl -u activity-system -f"
echo "             或 tail -f $APP_ROOT/logs/backend.log"
echo "冒烟测试   : bash $SCRIPT_DIR/smoke-test.sh $PUBLIC_IP"
echo "演示签到   : 报名完成后执行 mysql campus_activity < $SCRIPT_DIR/demo-checkin.sql"
echo "=================================================="
echo "别忘了在阿里云安全组放行 80 端口（以及 SSH 22，建议限制来源 IP）。"
