#!/usr/bin/env bash
# Linux 一键部署：修复权限（含 zip 解压后目录不可读）、准备 data / .env、启动 docker compose
# 部署根目录 = 执行时的当前工作目录（请先 cd 到含 docker-compose.yml 的项目根再运行）
# 示例：cd ~/expirement && bash deploy-linux.sh
# 可用 root 或普通用户执行
# 依赖：Docker（含 compose v2 插件）；非 root 时需 sudo（find + chmod + chown，避免无法进入子目录）

set -euo pipefail

log() { printf '%s\n' "$*"; }

die() { log "错误：$*"; exit 1; }

ROOT="$(pwd -P)"
[[ -f "$ROOT/docker-compose.yml" ]] || die "未在当前目录找到 docker-compose.yml。请先 cd 到项目根目录再执行。当前目录：$ROOT"

UID_="$(id -u)"
GID_="$(id -g)"

command -v docker >/dev/null 2>&1 || die "未找到 docker，请先安装 Docker Engine。"
docker compose version >/dev/null 2>&1 || die "未找到 docker compose（v2 插件）。"

if ! docker info >/dev/null 2>&1; then
  if [[ "$UID_" -ne 0 ]]; then
    die "无法连接 Docker 守护进程。可尝试：sudo usermod -aG docker \"$USER\" 后重新登录；或检查 Docker 服务是否已启动。"
  fi
  die "无法连接 Docker 守护进程。请检查 Docker 服务：systemctl status docker"
fi

# 先用 root 能力遍历整棵树并 chmod，再 chown；否则在目录缺 x 位时，普通用户 find 无法进入子目录会直接报错
fix_tree_permissions() {
  local base="$1"
  if [[ "$UID_" -eq 0 ]]; then
    find "$base" -type d -exec chmod u+rwx {} +
    find "$base" -type f -exec chmod u+rw {} +
    chown -R "${UID_}:${GID_}" "$base"
  else
    sudo find "$base" -type d -exec chmod u+rwx {} +
    sudo find "$base" -type f -exec chmod u+rw {} +
    sudo chown -R "${UID_}:${GID_}" "$base"
  fi
}

log "[1/4] 修正项目属主与权限（解决 unzip 后 permission denied）：$ROOT"
fix_tree_permissions "$ROOT"

log "[2/4] 创建数据持久化目录（./data）"
mkdir -p "$ROOT/data/postgres" "$ROOT/data/chat-files" "$ROOT/data/vector-store"
fix_tree_permissions "$ROOT/data"

log "[3/4] 环境变量文件 .env"
if [[ ! -f "$ROOT/.env" ]]; then
  if [[ -f "$ROOT/.env.example" ]]; then
    cp "$ROOT/.env.example" "$ROOT/.env"
    log "      已从 .env.example 生成 .env，请编辑并填写 SPRING_AI_OPENAI_API_KEY 等变量后再依赖 AI 能力。"
  else
    log "      警告：未找到 .env.example，请手动创建 $ROOT/.env。"
  fi
else
  log "      已存在 .env，不会覆盖。"
fi

log "[4/4] 构建镜像并启动容器"
docker compose -f "$ROOT/docker-compose.yml" up -d --build

log "完成。查看状态：docker compose ps"
log "      查看日志：docker compose logs -f --tail=50"
