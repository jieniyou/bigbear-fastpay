# FastPay 待修复问题与本地启动清单

> 记录时间：2026-08-21
> 当前策略：已开始修复高优先级安全问题，并补充 Docker Compose 部署方式；仍保留原始风险清单用于后续复查。

## 一、待修复安全问题清单

| 优先级 | 问题 | 位置 | 风险 | 建议修复方向 |
| --- | --- | --- | --- | --- |
| P0 | 初始化脚本存在默认管理员 `admin / 123456` | `fastpay-server/src/main/resources/db/init.sql` | 数据库初始化后如果直接部署，攻击者可尝试默认后台账号登录 | 删除默认账号，或改为首次启动强制设置随机强密码 |
| P0 | 启动时如果管理员表为空，会自动创建配置账号 `fastpay / 123456@` | `fastpay-server/src/main/java/com/fastpay/config/InitConfig.java`、`fastpay-server/src/main/java/com/fastpay/service/impl/AdminServiceImpl.java`、`fastpay-server/src/main/resources/application-dev.yml`、`fastpay-server/src/main/resources/application-prod.yml` | 清空管理员表或首次部署时会重新生成固定后台入口 | 改为环境变量注入强密码，或禁用自动创建默认管理员 |
| P0 | 管理员/商户角色校验可能因 `context-path` 失效 | `fastpay-server/src/main/java/com/fastpay/config/AuthInterceptor.java` | 在 `/fastpay-server` 上下文路径下，`requestURI` 带前缀，`startsWith("/api/admin/")` 可能匹配失败，导致有效 JWT 跨角色访问 | 使用 `request.getServletPath()`，或先移除 `request.getContextPath()` 再判断 |
| P0 | 商户订单详情接口未校验订单归属 | `fastpay-server/src/main/java/com/fastpay/controller/merchant/MerchantOrderController.java` | 商户可能通过订单号查看其他商户订单详情 | 按 `merchantId + orderNo` 查询，或查出订单后强制比对 `merchantId` |
| P1 | Knife4j/OpenAPI 文档生产环境默认启用且被鉴权放行 | `fastpay-server/src/main/resources/application-prod.yml`、`fastpay-server/src/main/java/com/fastpay/config/WebConfig.java` | 对外暴露接口结构，方便扫描和攻击 | 生产关闭 `knife4j.enable`，并在网关限制 `/doc.html`、`/v3/api-docs/**` |
| P1 | 配置中硬编码数据库密码、JWT 密钥、公网 IP、Demo API 密钥 | `fastpay-server/src/main/resources/application-prod.yml`、`fastpay-demo/src/main/resources/application-prod.yml`、前端 `vite.config.js` | 凭据泄露、误连作者公网服务、Token 可伪造风险 | 改用环境变量或外部配置文件，所有密钥重新生成 |
| P1 | 密码使用 MD5 存储与校验 | `AdminServiceImpl.java`、`MerchantServiceImpl.java`、`init.sql` | 弱哈希易被撞库或彩虹表破解 | 改用 BCrypt/Argon2，新旧密码做登录迁移 |
| P1 | CORS 配置为任意来源且允许凭据 | `fastpay-server/src/main/java/com/fastpay/config/WebConfig.java` | 增大跨站请求与 Token 被滥用风险 | 生产环境限制为实际管理后台和商户平台域名 |
| P2 | 公开状态接口返回接口清单 | `fastpay-server/src/main/java/com/fastpay/controller/IndexController.java` | 暴露系统信息和接口路径 | 生产环境隐藏 `/api/info` 或仅内网可访问 |
| P2 | 支付状态、结果、页面数据接口通过订单号公开查询 | `fastpay-server/src/main/java/com/fastpay/controller/pay/PayController.java` | 订单号泄露时可查看部分订单信息 | 对敏感字段脱敏，或增加签名/短期访问 token |
| P2 | WebSocket 连接仅校验商户存在，连接表按 `outTradeNo` 存储 | `fastpay-server/src/main/java/com/fastpay/websocket/PayResultWebSocket.java` | 不同商户订单号冲突时可能互相覆盖连接，且缺少连接签名 | 会话 key 改为 `merchantNo + outTradeNo`，并增加短期签名校验 |

## 二、本地启动是否必须改配置

本地启动基本不需要改业务代码，但需要改“运行配置”。最少需要处理三类：

1. 数据库连接：本地 MySQL 账号、密码、端口和库名。
2. 前端代理：当前前端开发代理默认指向公网 `121.4.28.146`，本地联调应改为 `localhost:7001`。
3. 支付/Demo 地址：如果要完整跑支付页面和 Demo，需要把公网域名、商户编号、店铺编号、API 密钥改成本地生成的数据。

另外，当前项目只有 `bootstrap.yml`，没有通用 `application.yml`。直接执行 `mvn spring-boot:run` 时 Spring Boot 不会自动按预期加载 `application-dev.yml` 或 `application-prod.yml`，本地启动必须显式指定 profile。

## 三、本地启动前置环境

| 组件 | 当前项目要求 | 本机建议 |
| --- | --- | --- |
| JDK | Java 17+ | 可使用当前默认 Java 21 |
| Maven | 3.8+ | 可使用当前 Maven 3.9.11 |
| Node.js | 18+ | 可使用当前 Node 20 |
| MySQL | 8.0+ | 优先使用本机 MySQL80，端口 `3306` |

## 四、后端配置位置

配置文件：`fastpay-server/src/main/resources/application-dev.yml`

需要重点检查：

| 配置项 | 当前含义 | 本地建议 |
| --- | --- | --- |
| `spring.datasource.url` | 数据库连接地址 | 如果使用本机 MySQL80 端口 `3306` 且库名 `bigbear_fastpay`，可保持库名和端口不变 |
| `spring.datasource.username` | 数据库用户名 | 填本机 MySQL8 用户名，例如 `root` |
| `spring.datasource.password` | 数据库密码 | 填本机 MySQL8 密码 |
| `fastpay.jwt.secret` | JWT 签名密钥 | 本地可暂用现值，生产必须更换 |
| `fastpay.admin.init-enabled` | 是否在空管理员表时创建首次管理员 | 默认 `false`，只有空库首次启动时临时改为 `true` |
| `fastpay.admin.username` | 空管理员表时创建的首次管理员账号 | 空库首次启动时填写自己知道的账号 |
| `fastpay.admin.password` | 空管理员表时创建的首次管理员密码 | 必须填至少 12 位强密码，不要使用默认密码 |
| `fastpay.pay.page-domain` | 支付页面前端地址 | 本地联调改为 `http://localhost:3002/fastpay-merchant` |
| `fastpay.notify.callback-url` | 监听软件回调地址展示值 | 本地联调改为 `http://localhost:7001/fastpay-server/api/notify/callback` |

示例：

```yaml
# 服务端口，本地后端访问地址为 http://localhost:7001/fastpay-server
server:
  port: 7001
  servlet:
    context-path: /fastpay-server

spring:
  datasource:
    # 本地 MySQL8，库名为 bigbear_fastpay
    url: jdbc:mysql://localhost:3306/bigbear_fastpay?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    # 本地数据库用户名
    username: root
    # 本地数据库密码
    password: 你的MySQL8密码

fastpay:
  jwt:
    # JWT 本地开发密钥，生产必须替换
    secret: FastPayLocalJwtSecretChangeMe2026
    # Token 有效期，单位：小时
    expire-hours: 12
  admin:
    # 是否启用首次管理员初始化；已有管理员账号时保持 false
    init-enabled: false
    # 空库首次启动时填写管理员用户名
    username:
    # 空库首次启动时填写至少 12 位强密码
    password:
  pay:
    # 订单超时时间，单位：分钟
    order-timeout-minutes: 3
    # 本地商户平台支付页面地址
    page-domain: http://localhost:3002/fastpay-merchant
  notify:
    # 本地监听软件回调地址
    callback-url: http://localhost:7001/fastpay-server/api/notify/callback
```

## 五、数据库初始化步骤

1. 确认 MySQL80 服务已启动，端口为 `3306`。
2. 使用数据库客户端或命令行登录 MySQL。
3. 执行初始化脚本：

```sql
-- 创建并使用项目数据库
CREATE DATABASE IF NOT EXISTS bigbear_fastpay DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 切换到项目数据库
USE bigbear_fastpay;

-- 执行项目初始化脚本
SOURCE D:/WorkSpace/wuyis/FastPay/fastpay-server/src/main/resources/db/init.sql;
```

注意：`init.sql` 自身也包含建库和 `USE bigbear_fastpay`，如果使用图形化工具，可以直接打开并执行整个脚本。

## 六、前端本地代理配置

### 管理后台

文件：`fastpay-admin/vite.config.js`

当前代理目标是公网：

```js
// 管理后台接口代理，当前指向公网
target: 'http://121.4.28.146:80'
```

本地应改为：

```js
// 管理后台接口代理，改为本地后端
target: 'http://localhost:7001'
```

### 商户平台

文件：`fastpay-merchant/vite.config.js`

当前 HTTP 和 WebSocket 代理目标是公网：

```js
// 商户平台 HTTP 接口代理，当前指向公网
target: 'http://121.4.28.146:80'

// 商户平台 WebSocket 代理，当前指向公网
target: 'ws://121.4.28.146:80'
```

本地应改为：

```js
// 商户平台 HTTP 接口代理，改为本地后端
target: 'http://localhost:7001'

// 商户平台 WebSocket 代理，改为本地后端
target: 'ws://localhost:7001'
```

## 七、启动顺序

### 1. 启动后端

```powershell
# 进入后端目录
cd D:\WorkSpace\wuyis\FastPay\fastpay-server

# 使用 dev 配置启动后端，PowerShell 中需要给 -D 参数加引号
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

后端地址：

- `http://localhost:7001/fastpay-server`
- 健康检查：`http://localhost:7001/fastpay-server/api/health`

### 2. 启动管理后台

```powershell
# 进入管理后台目录
cd D:\WorkSpace\wuyis\FastPay\fastpay-admin

# 首次启动安装依赖
npm install

# 启动开发服务
npm run dev
```

访问地址：

- `http://localhost:3001/fastpay-admin/`

### 3. 启动商户平台

```powershell
# 进入商户平台目录
cd D:\WorkSpace\wuyis\FastPay\fastpay-merchant

# 首次启动安装依赖
npm install

# 启动开发服务
npm run dev
```

访问地址：

- `http://localhost:3002/fastpay-merchant/`

### 4. 启动 Demo（可选）

文件：`fastpay-demo/src/main/resources/application-prod.yml`

如果要本地联调 Demo，需要先把这些值替换为本地商户平台中生成的数据：

| 配置项 | 本地取值方式 |
| --- | --- |
| `fastpay.merchant-no` | 登录商户平台后，在开发配置中查看 |
| `fastpay.shopNo` | 商户平台创建店铺后查看 |
| `fastpay.api-secret` | 商户平台开发配置中查看或重置 |
| `fastpay.gateway-url` | 改为 `http://localhost:7001/fastpay-server` |
| `fastpay.notify-url` | 本地无公网回调时可先填 `http://localhost:7002/pay/notify` |
| `fastpay.return-url` | 本地可填 `http://localhost:7002/pay/return` |

启动命令：

```powershell
# 进入 Demo 目录
cd D:\WorkSpace\wuyis\FastPay\fastpay-demo

# 启动 Demo，PowerShell 中需要给 -D 参数加引号
mvn spring-boot:run "-Dspring-boot.run.profiles=prod"
```

访问地址：

- `http://localhost:7002`

## 八、本地启动后的初始化操作

1. 登录管理后台。
2. 创建商户账号。
3. 登录商户平台。
4. 创建店铺。
5. 创建支付通道。
6. 上传或录入收款二维码。
7. 在商户平台开发配置中确认 `merchantNo`、`shopNo`、`apiSecret`。
8. 如果需要监听回调，配置 SmsForwarder 的回调地址为本地或内网穿透后的 `/fastpay-server/api/notify/callback`。

## 九、已验证事项

- `fastpay-server` 执行 `mvn -q -DskipTests compile` 通过。
- `fastpay-demo` 执行 `mvn -q -DskipTests compile` 通过。
- 本地 Java、Maven、Node 版本满足项目要求。
- 本地数据库已初始化，`bigbear_fastpay` 下已创建 6 张表。
- 后端已使用 `dev` 配置启动，健康检查 `http://localhost:7001/fastpay-server/api/health` 返回 `OK`。
- 管理后台已启动，访问地址为 `http://localhost:3001/fastpay-admin/`。
- 商户平台已启动，访问地址为 `http://localhost:3002/fastpay-merchant/`。
- Demo 已使用 `prod` 配置启动，访问地址为 `http://localhost:7002`。

## 十、本次安全修复进度

| 状态 | 问题 | 修复位置 | 说明 |
| --- | --- | --- | --- |
| 已修复 | 初始化 SQL 写死默认管理员 | `fastpay-server/src/main/resources/db/init.sql` | 已移除 `admin / 123456` 初始化数据，首次管理员改为运行时配置控制 |
| 已修复 | 空管理员表自动创建固定账号 | `fastpay-server/src/main/java/com/fastpay/service/impl/AdminServiceImpl.java` | 默认关闭自动初始化；只有 `FASTPAY_ADMIN_INIT_ENABLED=true` 且密码满足强度时才创建 |
| 已修复 | 管理员/商户角色校验受 `context-path` 影响 | `fastpay-server/src/main/java/com/fastpay/config/AuthInterceptor.java` | 改用 Servlet Path 校验角色路径 |
| 已修复 | 商户订单详情未校验归属 | `MerchantOrderController.java`、`PayOrderServiceImpl.java` | 商户端订单详情按 `merchantId + orderNo` 查询 |
| 已修复 | 生产配置硬编码密码、JWT、公网 IP | `fastpay-server/src/main/resources/application-prod.yml` | 已改为环境变量注入，Docker Compose 从 `.env` 传入 |
| 已修复 | 生产环境 Knife4j/OpenAPI 默认开放 | `application-prod.yml`、`.env` | 生产默认关闭 `knife4j`、`springdoc.api-docs` 和 `swagger-ui` |
| 已修复 | 密码使用 MD5 存储与校验 | `PasswordUtil.java`、`AdminServiceImpl.java`、`MerchantServiceImpl.java` | 新密码使用 BCrypt；历史 MD5 登录成功后自动升级 |
| 已修复 | CORS 任意来源且允许凭据 | `fastpay-server/src/main/java/com/fastpay/config/WebConfig.java` | 支持 `FASTPAY_CORS_ALLOWED_ORIGINS` 白名单，通配时不允许凭据 |
| 已修复 | `/api/info` 暴露接口清单 | `fastpay-server/src/main/java/com/fastpay/controller/IndexController.java` | 生产默认关闭，由 `FASTPAY_API_INFO_ENABLED` 控制 |
| 已修复 | 商户列表返回敏感字段 | `MerchantSecurityUtil.java`、商户相关 Controller | 批量列表隐藏密码和 API Secret；详情/重置仅隐藏密码以保留授权查看密钥能力 |
| 已修复 | WebSocket 仅按订单号保存连接 | `fastpay-server/src/main/java/com/fastpay/websocket/PayResultWebSocket.java` | 会话 key 改为 `merchantNo:outTradeNo`，超时清理按商户归属过滤 |
| 待继续 | 支付页面公开查询接口脱敏/签名 | `fastpay-server/src/main/java/com/fastpay/controller/pay/PayController.java` | 仍建议增加短期访问 token 或进一步脱敏返回字段 |

## 十一、Docker Compose 部署

默认 compose 直接拉取 GitHub Actions 构建好的镜像，包含统一入口网关、后端、管理端、商户端和 MySQL，不包含 `fastpay-demo`。Demo 是对接演示应用，不是平台运行必需组件，默认部署到服务器会增加额外入口和演示配置暴露面。服务器只需要 `docker-compose.yml` 和 `.env`，详见 `docs/GITHUB_IMAGE_DEPLOY.md`。

生产部署时，应用本身不需要配置域名；`.env` 只需要配置对外网关端口。域名解析或 HTTPS 证书属于服务器外层 Nginx/宝塔/1Panel/CDN 的工作。最终对外访问仍建议使用“一个域名 + 路径”的方式：

- 根域名：`https://你的域名/` 会自动跳转到商户端 `/fastpay-merchant/`
- 管理后台：`https://你的域名/fastpay-admin/`
- 商户端：`https://你的域名/fastpay-merchant/`
- 后端接口：`https://你的域名/fastpay-server/`

### 文件位置

| 文件 | 用途 |
| --- | --- |
| `.env` | Docker Compose 实际读取的本地变量文件，已加入 `.gitignore` |
| `.env.example` | 可提交/复制的变量模板 |
| `docker-compose.yml` | 编排 `fast_pay_gateway`、`fast_pay_mysql`、`fast_pay_server`、`fast_pay_admin`、`fast_pay_merchant` |
| `deploy/nginx/fastpay.conf` | 统一入口网关配置，将一个域名下的不同路径反代到内部容器 |
| `fastpay-server/Dockerfile` | 后端 Maven 构建和 JRE 运行镜像 |
| `fastpay-admin/Dockerfile`、`fastpay-admin/nginx.conf` | 管理端构建并通过 Nginx 发布 |
| `fastpay-merchant/Dockerfile`、`fastpay-merchant/nginx.conf` | 商户端构建并通过 Nginx 发布，包含 WebSocket 代理 |

### 服务器部署前必须修改 `.env`

| 配置项 | 示例 | 说明 |
| --- | --- | --- |
| `FASTPAY_IMAGE_PREFIX` | `ghcr.io/jieniyou/bigbear-fastpay` | GitHub Actions 推送的镜像前缀 |
| `FASTPAY_IMAGE_TAG` | `latest` | 服务器拉取的镜像标签 |
| `GATEWAY_HTTP_PORT` | `80` | 对外 HTTP 入口端口；后端、管理端、商户端不再单独暴露公网端口 |
| `MYSQL_DATABASE` | `bigbear_fastpay` | 初始化脚本默认库名，建议保持不变；如需修改，需要同步调整 `init.sql` |
| `FASTPAY_JWT_SECRET` | 至少 32 位随机字符串 | JWT 签名密钥，生产必须重新生成 |
| `FASTPAY_ADMIN_USERNAME` | `fastpay_admin` | 首次启动创建的管理员账号 |
| `FASTPAY_ADMIN_PASSWORD` | 至少 12 位强密码 | 首次启动创建的管理员密码 |
| `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD` | 强随机密码 | MySQL root 和业务用户密码 |

### 初始化和启动

```powershell
# 进入项目根目录
cd D:\WorkSpace\wuyis\FastPay

# 检查 compose 文件和变量解析
docker compose config

# 拉取 GitHub Actions 构建好的镜像
docker compose pull

# 后台启动
docker compose up -d

# 查看容器状态
docker compose ps

# 查看后端日志
docker compose logs -f fast_pay_server
```

启动后访问：

- 根路径：访问网关 `/` 会自动跳转到 `/fastpay-merchant/`
- 管理后台：`/fastpay-admin/`
- 商户端：`/fastpay-merchant/`
- 后端健康检查：`/fastpay-server/api/health`

公网入口有两种常见做法：

1. 服务器只跑本 compose：`GATEWAY_HTTP_PORT=80`，公网访问服务器 80 端口即可；如果绑定域名，只需要把域名 A 记录指向服务器 IP。
2. 服务器已有宝塔/1Panel/Nginx 或需要 HTTPS：可把 `GATEWAY_HTTP_PORT` 改成内网端口（例如 `18080`），再由外部 Nginx/面板把站点反代到 `http://127.0.0.1:18080`。此时应用仍只关心端口，域名和证书由外层处理。

首次启动时，MySQL 容器会自动执行 `fastpay-server/src/main/resources/db/init.sql`。如果数据库卷 `fast_pay_mysql_data` 已存在，MySQL 不会重复执行初始化脚本。

### 本次验证记录

- 已通过：`fastpay-server` 执行 `mvn -q -DskipTests compile`。
- 已通过：`fastpay-demo` 执行 `mvn -q -DskipTests compile`。
- 已通过：`fastpay-admin` 执行 `npm run build`。
- 已通过：`fastpay-merchant` 执行 `npm run build`。
- 已检查：后端和 Demo 配置中不再保留原公网 IP、演示商户号、演示 API Secret 或固定默认管理员账号。
- 未执行：当前本机未检测到 `docker` / `docker-compose` 命令，`docker compose pull` 和 `docker compose up -d` 需要在已安装 Docker 的服务器上执行。

### 订单客户端 IP 截断修复

支付下单会经过 Nginx/CDN 等反向代理，`X-Forwarded-For` 可能包含多个地址。服务端现在只提取第一个合法 IPv4/IPv6 地址写入 `fp_pay_order.client_ip`，不再把整段代理链保存到订单。数据库字段已统一为 `VARCHAR(64)`。

已有数据库升级时，在目标数据库执行：

```sql
-- 将旧的 client_ip 字段扩容，并与新初始化结构保持一致。
SOURCE fastpay-server/src/main/resources/db/002_fix_client_ip.sql;
```

Docker 镜像构建也会把该迁移复制到 MySQL 初始化目录；已有数据卷不会自动重复执行初始化脚本，线上已有卷必须手动执行上述迁移后再发布服务端镜像。

### 邮件事件模板与一次性按钮迁移

订单邮件现在按事件维护邮件主题和 HTML 模板，并新增 `订单确认通知`、`订单关闭通知` 两个结果通知事件。邮件中的确认/关闭按钮由模板占位符 `{{action_buttons}}`、`{{confirm_button}}`、`{{close_button}}` 控制；链接写入 `fp_mail_action_token` 后只能消费一次，同一订单任一按钮使用后其它按钮同步失效。

已有数据库升级时，在目标数据库执行：

```sql
-- 创建邮件操作 Token 表，并补齐邮件事件配置键。
SOURCE fastpay-server/src/main/resources/db/003_mail_event_templates.sql;
```

`平台外部访问地址` 可以留空；留空时系统会按当前请求域名生成邮件中的商户端订单链接和操作链接。已有 Docker 数据卷同样不会自动重复执行初始化脚本，线上已有卷需要手动执行该迁移。
