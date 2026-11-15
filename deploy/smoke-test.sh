#!/usr/bin/env bash
# 部署后冒烟测试。用法：bash smoke-test.sh <公网IP>（服务器上不带参数则测 127.0.0.1）
set -euo pipefail

IP="${1:-127.0.0.1}"
B="http://$IP"

ok()   { echo "   [OK] $1"; }
bad()  { echo "   [FAIL] $1"; exit 1; }

echo "1) 前端首页"
code="$(curl -s -o /dev/null -w '%{http_code}' "$B/")"
[[ "$code" == "200" ]] && ok "HTTP $code" || bad "HTTP $code（检查 Nginx 是否占用 80、安全组是否放行）"

echo "2) history 深链 /activity/1 刷新"
code="$(curl -s -o /dev/null -w '%{http_code}' "$B/activity/1")"
[[ "$code" == "200" ]] && ok "HTTP $code" || bad "HTTP $code（try_files 回退没生效）"

echo "3) 移动端签到页 /m/checkin/1"
code="$(curl -s -o /dev/null -w '%{http_code}' "$B/m/checkin/1")"
[[ "$code" == "200" ]] && ok "HTTP $code" || bad "HTTP $code"

echo "4) 静态资源 gzip（1M 带宽的关键）"
JS="$(curl -fsS "$B/" | grep -o '/assets/[^"]*\.js' | head -1 || true)"
if [[ -n "$JS" ]]; then
  if curl -s -H 'Accept-Encoding: gzip' -D - -o /dev/null "$B$JS" | grep -qi 'Content-Encoding: gzip'; then
    ok "$JS 已压缩"
  else
    bad "$JS 未压缩（检查 nginx.conf 的 gzip_types）"
  fi
else
  bad "首页里没找到 assets 下的 js"
fi

echo "5) 字典接口 /api/v1/dict/activity-type"
resp="$(curl -fsS "$B/api/v1/dict/activity-type")"
echo "$resp" | grep -q '"code":0' && ok "返回 code:0" || bad "接口异常: ${resp:0:200}"

echo "6) 登录接口 /api/v1/auth/login"
resp="$(curl -fsS -X POST "$B/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}')"
if echo "$resp" | grep -q '"code":0'; then
  ok "admin / admin123 可登录（请立刻到账号管理改密码）"
else
  echo "   [WARN] 登录未按默认密码通过：${resp:0:200}"
  echo "          若你已修改过密码，这是正常的。"
fi

echo
echo "冒烟完成。浏览器打开 $B 即可。"
echo "提醒：本方案是 HTTP，浏览器不给定位权限，签到请用"
echo "      $B/m/checkin/<活动ID>  （不带 ?code=），走基本签到。"
