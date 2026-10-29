# 校园保卫安防管理平台（SaaS）

> 面向中小学、高校的一体化校园安全 SaaS：门禁访客、车辆与车牌识别、治安事件、消防隐患、巡逻巡更、失物招领、安全教育、报备审批、设备运维、应急管理、排班考勤与多端消息触达。

- 生产域名：`wx-bwc-api.greathiit.com`
- 微信合法域名：`wx-bwc-api.greathiit.com`
- 后台入口：`https://wx-bwc-api.greathiit.com/admin/`
- H5 门户：`https://wx-bwc-api.greathiit.com/h5/`
- 接口前缀：`https://wx-bwc-api.greathiit.com/api`

## 一、产品矩阵

| 端 | 目录 | 技术 | 主要使用者 |
|---|---|---|---|
| 服务端 API | `backend/` | Java 17、Spring Boot 3.2、MyBatis-Plus、MySQL 8、Redis、RabbitMQ | 全部端 |
| PC 管理后台 | `admin-web/` | Vue 3、Vite 4、Element Plus、ECharts | 平台超管、租户管理员、业务角色 |
| 微信小程序 | `miniprogram/` | 微信原生小程序 | 师生、访客、保安、被访人 |
| 微信公众号 | `backend/`（回调与菜单） | 微信服务号 API | 师生、家长 |
| H5 公开门户 | `portal-web/` | Vue 3、Vue Router、vue-i18n、SEO | 访客、家长、公众 |
| 独立 App | `app/` | Flutter 3 | 保安一线、管理端、校领导 |
| 数据库脚本 | `sql/` | MySQL 8 | 部署实施 |
| 部署配置 | `deploy/` | Nginx、systemd、Docker Compose、K8s/K3s | 运维 |

## 二、核心能力

- **SaaS 行级隔离**：所有业务表自动注入 `tenant_id`；平台表白名单独立；平台超管可跨租户只读下钻。
- **两级 RBAC**：`SYSTEM` 平台角色 + 租户角色（租户管理员/保安队长/保安/教职工/学生/校领导/被访人），菜单与接口双重校验。
- **安全合规**：身份证号、手机号、微信密钥、车牌识别 API Key AES 加密存储，页面脱敏；操作全量审计。
- **业务闭环**：预约→审批→通行码→门岗核验→通行台账；隐患上报→分派→整改→验收；事件接警→处置→结案；失物登记→公告→认领→处置。
- **微信底座**：UnionID 多端互通、公众号菜单同步、模板消息/订阅消息、回调与发送日志、站内消息兜底。
- **车牌识别对接**：BASE 地址/API Key 可视化配置、连接测试、相机台账、固定/临时车牌下发、失败重试、进出日志与对账。
- **多端导航可视化**：后台菜单同时驱动 PC 侧边栏、小程序 tabBar、H5 导航、App 快捷入口。
- **离线能力**：App 巡逻/巡检弱网离线记录，联网补传。
- **报表与打印**：租户/平台多维报表、Excel 导出、通行码/审批单/整改单/失物标签/月报 PDF。

## 三、目录结构

```text
.
├── backend/                 Spring Boot 服务端
│   ├── src/main/java/com/greathiit/bwc/
│   │   ├── common/          统一响应、CRUD、加密、编号、PDF/Excel/二维码
│   │   ├── config/          租户插件、安全、Redis、RabbitMQ、存储
│   │   ├── security/        JWT、权限、数据范围、审计
│   │   └── module/          system/visitor/vehicle/event/fire/patrol/lost/
│   │                        education/report/device/emergency/logistics
│   └── src/main/resources/application.yml
├── admin-web/               Vue3 管理后台
├── miniprogram/             微信原生小程序
├── portal-web/              H5 公开门户
├── app/                     Flutter 独立 App
├── sql/schema.sql           78 张表结构
├── sql/data.sql             租户/角色/菜单/字典/示例数据
├── deploy/                  Nginx/systemd/Docker/K8s 配置
├── docs/                    技术文档与六端用户手册
├── scripts/                 实体/配置生成脚本
├── tasks.md                 任务清单 T01~T78
├── checklist.md             上线验收清单
├── PRO.md                   产品与架构方案
└── CLAUDE.md                编码准则（原样交付）
```

## 四、技术栈

| 层 | 选型 |
|---|---|
| 语言/运行时 | Java 17、Node 16+、Flutter 3 |
| 服务框架 | Spring Boot 3.2.5、Spring Security、Validation、AOP |
| 持久层 | MyBatis-Plus 3.5.x（租户插件、逻辑删除、分页）、MySQL 8.0 |
| 缓存/消息 | Redis 7、RabbitMQ 3.13 |
| 存储 | 本地、S3、阿里 OSS、腾讯 COS、MinIO 五种驱动 |
| 文档/导出 | PDFBox、Apache POI、ZXing |
| 后台 | Vue 3、Vite 4、Element Plus、ECharts、WangEditor |
| H5 | Vue 3、Vue Router、vue-i18n、SEO hreflang |
| App | Flutter、Provider、http、geolocator、mobile_scanner、shared_preferences |
| 部署 | Nginx、systemd、Docker Compose、K8s/K3s |

## 五、本地快速开始

### 1. 环境要求

- JDK 17、Maven 3.8+
- MySQL 8.0（`utf8mb4`）、Redis 7、RabbitMQ 3.13（可选）
- Node.js 16+、npm 8+
- Flutter 3.16+（仅 App 开发需要）

### 2. 初始化数据库

```bash
mysql -h127.0.0.1 -uroot -p < sql/schema.sql
mysql -h127.0.0.1 -uroot -p bwc < sql/data.sql
```

### 3. 启动后端

```bash
cd backend
mvn -DskipTests package
java -jar target/bwc-api.jar
```

默认端口 `8080`，可通过环境变量覆盖：

```bash
DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=bwc DB_USERNAME=root DB_PASSWORD=yourpass \
REDIS_HOST=127.0.0.1 SERVER_PORT=8080 java -jar target/bwc-api.jar
```

健康检查：`GET http://127.0.0.1:8080/api/public/health`

### 4. 启动后台与 H5

```bash
cd admin-web && npm install && npm run dev      # http://localhost:5173/admin/
cd portal-web && npm install && npm run dev     # http://localhost:5174/h5/
```

生产构建：

```bash
cd admin-web && npm run build
cd portal-web && npm run build
```

### 5. 小程序 / App

- 小程序：微信开发者工具导入 `miniprogram/`，将 `utils/config.js` 的 `BASE_URL` 指向后端；生产域名 `wx-bwc-api.greathiit.com`。
- App：`cd app && flutter pub get`，首次按 `app/README.md` 生成原生工程后运行。

## 六、默认账号

初始化数据统一密码：`Admin@123`

| 端 | 账号 | 租户编码 | 说明 |
|---|---|---|---|
| 平台超管 | `system` | - | 跨租户看板、租户管理、平台参数 |
| 示范中学管理员 | `demo_admin` | `DEMO` | 租户全量业务 |
| 保安队长 | `demo_captain` | `DEMO` | 巡逻、事件、审批 |
| 保安 | `demo_guard` | `DEMO` | 一线执勤 |
| 教职工 | `demo_teacher` | `DEMO` | 被访人审批 |
| 学生 | `demo_student` | `DEMO` | 预约、失物、教育 |
| 校领导 | `demo_leader` | `DEMO` | 只读看板 |

> 首次部署后请立即修改默认密码，并更换 `JWT_SECRET`、`AES_KEY`。

## 七、配置说明

主要环境变量见 `deploy/.env.example`：

- 数据库：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`
- Redis：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`
- RabbitMQ：`RABBITMQ_*`、`BWC_MQ_LISTENER`
- 安全：`JWT_SECRET`、`JWT_EXPIRE_HOURS`、`AES_KEY`
- 上传：`UPLOAD_PATH`、`UPLOAD_MAX_MB`
- 微信：`WX_MP_APP_ID`、`WX_MP_APP_SECRET`、`WX_MINI_APP_ID`、`WX_MINI_APP_SECRET`
- 车牌识别：`PLATE_TIMEOUT_SECONDS`、`PLATE_BASE_URL`、`PLATE_API_KEY`
- PDF 字体：`BWC_PDF_FONT`（默认 `/usr/share/fonts/chinese/stsong.ttf`）

对象存储、微信 AppID/Secret、车牌识别 BASE/Key 也可在后台可视化配置，数据库中密钥字段加密保存。

## 八、交付清单

- [x] 后端源码与 SQL 初始化脚本
- [x] Vue3 PC 管理后台（78 资源通用 CRUD + 专项业务流程页）
- [x] 微信原生小程序（访客/车辆/失物/报修/教育/审批/消息/个人中心）
- [x] H5 公开门户（SEO、访客预约、失物、公告、分享落地页）
- [x] Flutter 独立 App（保安一线/管理/校领导看板/离线打卡/多语言）
- [x] Nginx、systemd、Docker Compose、K8s/K3s 配置
- [x] `架构设计.md`、`数据库设计.md`、`API接口.md`、`部署与运维.md`
- [x] 六端用户使用说明书（`docs/`）
- [x] `CLAUDE.md` 原样交付
- [x] `checklist.md` 验收框架

## 九、文档索引

- [架构设计](架构设计.md)
- [数据库设计](数据库设计.md)
- [API 接口](API接口.md)
- [部署与运维](部署与运维.md)
- [PC 后台手册](docs/用户手册-PC后台.md)
- [保安一线端手册](docs/用户手册-保安一线端.md)
- [小程序手册](docs/用户手册-小程序.md)
- [公众号手册](docs/用户手册-公众号.md)
- [H5 门户手册](docs/用户手册-H5门户.md)
- [独立 App 手册](docs/用户手册-独立App.md)

## 十、版本记录

| 版本 | 日期 | 说明 |
|---|---|---|
| 1.0.0 | 2026-10-29 | 首个完整交付版本：T01~T78 全量实现 |

## 十一、外部条件说明

以下能力代码与配置已就绪，端到端验收依赖外部资质/设备：

- 微信服务号认证、开放平台 UnionID、模板消息/订阅消息模板 ID、真机
- 车牌识别相机与厂商 API 网关
- 短信/支付等第三方通道（按需开通）

未配置时系统会明确报错或降级为站内消息，不影响主业务闭环。
