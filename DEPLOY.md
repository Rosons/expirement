# 部署说明

本文说明如何在 Linux / Windows 上使用 **Docker Compose** 部署本仓库，并补充打包、权限、端口与环境变量注意事项。

## 1. 前置条件

- **Docker Engine** 已安装且守护进程可访问（`docker info` 无报错）。
- **Docker Compose v2**：`docker compose version` 可执行（注意是 `docker compose` 子命令，不是旧版独立 `docker-compose` 二进制）。
- 磁盘空间：镜像与 `./data` 持久化目录需预留足够空间。

## 2. 标准部署流程

### 2.1 获取代码

```bash
git clone <你的仓库地址>
cd expirement   # 以实际目录名为准
```

### 2.2 环境变量

在**项目根目录**（与 `docker-compose.yml` 同级）：

```bash
cp .env.example .env
```

编辑 `.env`，**至少**设置：

- `SPRING_AI_OPENAI_API_KEY`：兼容 OpenAI 的 API Key（勿提交到 Git）。

其余变量（端口、数据库、模型名、RAG 参数、`VITE_*` 等）见 `.env.example` 内注释。

### 2.3 启动

```bash
docker compose up -d --build
```

查看状态与日志：

```bash
docker compose ps
docker compose logs -f --tail=50
```

### 2.4 访问地址

| 用途 | 默认说明 |
| --- | --- |
| 前端（Nginx） | `http://<宿主机>:<FRONTEND_HTTP_PORT>`，默认 `FRONTEND_HTTP_PORT=80` |
| 后端直连 | `http://<宿主机>:<BACKEND_HTTP_PORT>`，默认映射到容器内 `8080` |
| PostgreSQL | 默认映射 `POSTGRES_PUBLISH_PORT=5432`；生产可取消映射，仅容器内访问 |

## 3. 数据持久化

与 `docker-compose.yml` 同级的 **`./data`** 目录：

- `data/postgres`：PostgreSQL 数据文件  
- `data/chat-files`：聊天相关文件存储  
- `data/vector-store`：向量持久化文件  

首次启动前目录可不存在，Compose 会随容器创建；**备份与迁移时请整体备份 `data`**（注意权限与 Postgres 一致性）。

## 4. Linux 一键脚本（可选）

仓库根目录提供 **`deploy-linux.sh`**，适用于解压 zip 后出现**目录不可执行 / 权限异常**、或希望自动创建 `data` 与 `.env` 的场景。

用法：**必须先 `cd` 到含 `docker-compose.yml` 的项目根**，再执行：

```bash
cd /path/to/expirement
bash deploy-linux.sh
```

脚本会：

1. 修正当前项目树权限（必要时使用 `sudo`）  
2. 创建 `./data` 下子目录  
3. 若不存在 `.env` 且存在 `.env.example`，则复制生成 `.env`  
4. 执行 `docker compose up -d --build`  

若 Docker 需非 root 用户加入 `docker` 组，请按脚本提示处理。

## 5. 部署包打包（可选）

仓库提供两套等价脚本，都会在项目根下生成 **`deploy-packages/expirement-docker-deploy-*.zip`**，排除内容一致（`.git`、`node_modules`、`target`、`dist`、`data`、常见 IDE 目录、`deploy-packages` 等），便于上传到服务器解压后按第 2 节配置 `.env` 再启动。

| 环境 | 脚本 | 依赖 |
| --- | --- | --- |
| **Windows** | `scripts/package-docker-deploy.ps1` | PowerShell 5.1+，系统自带 **robocopy**；使用 `Compress-Archive` 生成 zip |
| **Linux / macOS** | `scripts/package-docker-deploy.sh` | Bash、**rsync**、**zip** |

若仅在本机部署、不需要上传服务器，可**跳过打包**，直接在项目根使用第 2 节的 `docker compose`。

### 5.1 Windows（PowerShell）

在**项目根目录**打开 PowerShell，执行其一即可：

```powershell
# 若在仓库根目录
powershell -ExecutionPolicy Bypass -File .\scripts\package-docker-deploy.ps1
```

或先 `cd` 到仓库根后：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\scripts\package-docker-deploy.ps1
```

成功后在控制台输出 `OK:` 与 zip 完整路径。若脚本执行策略受限，可使用 `-ExecutionPolicy Bypass`（见脚本文件头注释）。

### 5.2 Linux / macOS（Bash）

在**已安装 `rsync` 与 `zip`** 的环境中，于项目根目录执行：

```bash
chmod +x scripts/package-docker-deploy.sh
./scripts/package-docker-deploy.sh
```

WSL（Ubuntu 等）若缺少命令，可先安装：`sudo apt update && sudo apt install -y rsync zip`。

### 5.3 其他说明

- 在 **WSL** 下既可使用 **5.2** 的 `.sh`，也可在 Windows 侧用 **5.1** 的 `.ps1` 打包同一工作副本（注意路径与换行符差异）。
- 若无法运行脚本，可用手动压缩工具打包并**排除**上述目录，效果与脚本一致。

## 6. Windows 说明

- 使用 **Docker Desktop** 时，在 PowerShell 或 CMD 中于项目根目录执行：  
  `copy .env.example .env`，编辑后执行：  
  `docker compose up -d --build`  
- 路径与卷挂载使用与 Linux 一致的相对路径 `./data`；确保盘符空间充足。
- **`deploy-linux.sh`** 面向 Linux 解压权限等问题，Windows 本地部署可忽略。
- **部署包 zip**：在 Windows 上请使用 **第 5.1 节** 的 `package-docker-deploy.ps1`。

## 7. 端口与安全建议

- **生产环境**：建议将 PostgreSQL 的 `ports` 整段注释或收紧防火墙，仅允许应用容器访问数据库。
- **HTTPS**：当前 Compose 示例为 HTTP；对外暴露请在前方加反向代理或负载均衡并配置 TLS。
- **密钥**：`.env` 仅保存在服务器或本地安全位置；CI 中使用密钥管理或机密注入，勿写入镜像层。

## 8. 常见问题

| 现象 | 可能原因与处理 |
| --- | --- |
| `docker compose` 报未找到 | 安装 Docker Compose v2 插件，或使用新版 Docker Desktop。 |
| 无法连接 Docker 守护进程 | Linux：检查 `systemctl status docker`；用户加入 `docker` 组后重新登录。 |
| 解压 zip 后 `permission denied` | Linux 下执行 `deploy-linux.sh`，或手动对项目根与 `data` 修正权限与属主。 |
| 上传文件返回 413 | Nginx 已配置 `client_max_body_size 100m`，与后端 multipart 上限一致；若前有其他代理，需同步调大。 |
| 流式输出异常 | 确保经 Nginx 的路径使用配置中的 `proxy_buffering off` 等（本仓库 `portal-frontend/nginx/default.conf` 已处理）。 |

## 9. 更新与重建

修改代码后重新构建并启动：

```bash
docker compose up -d --build
```

仅变更 `.env` 时，通常需 `docker compose up -d` 使容器重新读取环境（视修改项而定；变更构建参数 `VITE_*` 需重新 **build** 前端镜像）。

---

更细的变量列表以 **`.env.example`** 为准；与 Spring 配置对应关系见 `spring-ai-demo/src/main/resources/application.yaml`。
