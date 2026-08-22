# FastPay GitHub 镜像部署说明

本部署方式适合低内存服务器：GitHub Actions 负责构建镜像，服务器只负责拉镜像和运行容器。

## 1. GitHub 构建镜像

推送代码到 `master` 或 `main` 后，GitHub Actions 会自动构建并推送以下镜像到 GHCR：

- `ghcr.io/jieniyou/bigbear-fastpay-gateway:latest`
- `ghcr.io/jieniyou/bigbear-fastpay-mysql:latest`
- `ghcr.io/jieniyou/bigbear-fastpay-server:latest`
- `ghcr.io/jieniyou/bigbear-fastpay-admin:latest`
- `ghcr.io/jieniyou/bigbear-fastpay-merchant:latest`

对应 workflow：`.github/workflows/docker-images.yml`。

如果 GHCR 镜像不是公开包，需要在 GitHub Packages 页面把这些 package 设置为 Public；否则服务器拉取前需要执行 `docker login ghcr.io`。

## 2. 服务器只需要两个文件

服务器目录只需要：

- `docker-compose.yml`
- `.env`

不需要上传 `fastpay-server`、`fastpay-admin`、`fastpay-merchant` 等源码目录。

## 3. .env 必填项

```env
FASTPAY_IMAGE_PREFIX=ghcr.io/jieniyou/bigbear-fastpay
FASTPAY_IMAGE_TAG=latest
GATEWAY_HTTP_PORT=18088

MYSQL_ROOT_PASSWORD=改成强密码
MYSQL_DATABASE=bigbear_fastpay
MYSQL_USER=fastpay
MYSQL_PASSWORD=改成强密码

FASTPAY_JWT_SECRET=改成至少32位强随机字符串
FASTPAY_JWT_EXPIRE_HOURS=12
FASTPAY_ADMIN_INIT_ENABLED=true
FASTPAY_ADMIN_USERNAME=fastpay_admin
FASTPAY_ADMIN_PASSWORD=改成至少12位强密码

FASTPAY_ORDER_TIMEOUT_MINUTES=3
FASTPAY_API_INFO_ENABLED=false
KNIFE4J_ENABLED=false
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
```

## 4. 启动命令

```bash
cd /你的部署目录

docker compose pull

docker compose up -d

docker compose ps
```

查看日志：

```bash
docker compose logs -f fast_pay_server
```

## 5. 访问方式

如果没有外部 Nginx/宝塔/1Panel：

```text
http://服务器IP:GATEWAY_HTTP_PORT/
```

如果有外部 Nginx/宝塔/1Panel/HTTPS：

- 外部站点反代到 `http://127.0.0.1:GATEWAY_HTTP_PORT`
- 用户访问正式域名即可
- 应用会根据请求 Host 自动生成支付页地址和回调地址

根路径 `/` 会自动跳转到 `/fastpay-merchant/`。

## 6. 首次管理员创建后

确认可以登录管理后台后，建议把 `.env` 改成：

```env
FASTPAY_ADMIN_INIT_ENABLED=false
```

然后重启后端：

```bash
docker compose up -d fast_pay_server
```

## 7. 更新版本

以后 GitHub Actions 构建出新镜像后，服务器只需要：

```bash
docker compose pull
docker compose up -d
```
