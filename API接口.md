# API 接口文档

> 校园保卫安防管理平台（SaaS）· 服务端 HTTP API
> 版本：1.0.0　最后更新：2026-10-29
> 生产地址：`https://wx-bwc-api.greathiit.com/api`
> 微信合法域名：`wx-bwc-api.greathiit.com`

本文档与 `架构设计.md`、`数据库设计.md`、`部署与运维.md` 同源，覆盖平台端、租户端、公开门户、微信回调及车牌识别对接接口。请求/响应字段以代码实体为准，新增字段遵循向后兼容原则。

---

## 一、通用约定

### 1.1 协议与编码

| 项 | 约定 |
|---|---|
| 协议 | HTTPS（生产强制），HTTP（仅本地调试） |
| 编码 | UTF-8，`Content-Type: application/json` |
| 时间 | `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai` |
| 文件上传 | `multipart/form-data`，单文件 ≤ 20 MB，单请求 ≤ 50 MB |
| 文件下载 | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` / `application/pdf` / `image/png` |

### 1.2 统一响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1761700000000
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | `0` 成功；非 0 为业务错误码 |
| message | string | 提示信息，可直接展示 |
| data | any | 业务数据，可能为对象、数组、分页对象或 `null` |
| timestamp | long | 服务端毫秒时间戳 |

分页响应的 `data` 结构：

```json
{
  "records": [],
  "total": 128,
  "size": 10,
  "current": 1,
  "pages": 13
}
```

### 1.3 认证

除公开接口外，所有请求必须携带 JWT：

```http
Authorization: Bearer <token>
```

令牌通过 `POST /api/auth/login` 或 `POST /api/auth/wx-login` 获取，默认有效期 24 小时（`JWT_EXPIRE_HOURS`）。服务端无状态，不维护会话；退出登录由客户端删除令牌。

### 1.4 租户与平台下钻

- 租户用户：`tenant_id` 从 JWT 解析，由 MyBatis-Plus 租户插件自动注入查询条件，客户端无需也不能指定。
- 平台用户：默认跨租户只读查询；如需下钻某校，增加请求头：

```http
X-Tenant-Id: 2
```

  下钻模式为**只读**，写操作返回 `403`；平台账号不可直接写业务数据。平台自身数据（租户、平台用户、全局字典、平台参数、语言、菜单）不在租户隔离范围。
- 越权访问统一返回 `403` 并写入审计日志。

### 1.5 错误码

| HTTP | code | 含义 | 处理建议 |
|---|---|---|---|
| 200 | 0 | 成功 | - |
| 400 | 400 | 参数校验失败/业务校验失败 | 展示 `message`，修正入参 |
| 401 | 401 | 未登录、令牌失效、账号密码错误 | 重新登录 |
| 403 | 403 | 越权、租户停用、只读下钻写操作 | 提示无权限，联系管理员 |
| 404 | 404 | 数据不存在或无权访问（避免探测） | 刷新列表 |
| 429 | 429 | 触发限流/防重 | 稍后重试 |
| 502 | 502 | 第三方（车牌识别/微信）调用失败 | 检查配置与网络 |
| 500 | 500 | 未处理异常 | 联系运维查看日志 |

### 1.6 限流与防重

基于 Redis 固定窗口计数，超限返回 `429`。主要策略：

| 场景 | 限制 |
|---|---|
| 账号登录 | 10 次/分钟/用户名 |
| 微信登录 | 30 次/分钟 |
| 微信绑定 | 10 次/分钟 |
| 公开访客预约 | 30 次/10 分钟/租户 |
| 公开失物登记 | 20 次/10 分钟/租户 |
| 审批、上报、车牌下发 | 业务层防重 + 分布式锁，重复请求返回明确提示 |

### 1.7 权限标识

权限格式 `模块:动作`，动作缺省为 `read`。两级角色：

- 平台：`SYSTEM`、`PLATFORM_OPERATOR`
- 租户：`TENANT_ADMIN`、`CAPTAIN`、`GUARD`、`STAFF`、`STUDENT`、`LEADER`、`VISITEE`

常用权限：`visitor:read/write/approve`、`vehicle:*`、`plate:*`、`event:*`、`fire:*`、`hazard:*`、`patrol:*`、`lost:*`、`education:*`、`report:*`、`device:*`、`emergency:*`、`logistics:*`、`feedback:*`、`system:*`、`tenant:*`、`platform:*`、`dict:*`、`param:*`、`menu:*`、`i18n:*`、`wechat:*`、`storage:*`、`audit:*`、`dashboard:*`。

---

## 二、公开接口（无需登录）

基础路径：`/api/public`，同时兼容 `/api/portal`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/health` | 健康检查 |
| GET | `/tenant/{code}` | 学校门户基础信息 + 租户配置 |
| GET | `/tenant/{code}/menus?platform=h5` | 门户导航；`platform` 可为 `h5`/`miniprogram`/`app` |
| GET | `/tenant/{code}/notices?limit=20` | 已发布公告列表 |
| GET | `/tenant/{code}/notices/{id}` | 公告详情（浏览量 +1） |
| GET | `/tenant/{code}/warnings` | 已发布安全预警 |
| GET | `/tenant/{code}/lost?category=&keyword=` | 公开失物列表 |
| GET | `/tenant/{code}/lost-notices` | 失物公告 |
| POST | `/tenant/{code}/visitor-appointment` | 公开访客预约 |
| POST | `/tenant/{code}/lost-report` | 公开捡拾登记 |
| GET | `/tenant/{code}/share/{type}/{id}` | 分享落地页元数据，`type`=`visitor`/`lost`/`warning` |

### 2.1 公开访客预约

`POST /api/public/tenant/{code}/visitor-appointment`

```json
{
  "visitorName": "张三",
  "idCard": "110101199001011234",
  "phone": "13800138000",
  "reason": "家长会",
  "visitTime": "2026-11-01 09:00:00",
  "leaveTime": "2026-11-01 11:00:00",
  "accompanyCount": 1,
  "plateNo": "京A12345",
  "visitUserId": 12,
  "visitUserName": "李老师",
  "remark": "携带资料"
}
```

响应仅返回脱敏信息：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1001,
    "visitorName": "张三",
    "phoneMask": "138****8000",
    "idCardMask": "110101********1234",
    "visitTime": "2026-11-01 09:00:00",
    "status": "PENDING",
    "message": "预约已提交，请等待被访人审批；审批通过后凭通行码入校。"
  }
}
```

### 2.2 公开捡拾登记

`POST /api/public/tenant/{code}/lost-report`

```json
{
  "name": "黑色雨伞",
  "category": "DAILY",
  "location": "图书馆一楼",
  "description": "长柄，伞面有白色条纹",
  "finder": "王同学",
  "storageLocation": "保卫处失物招领柜 A-03"
}
```

响应：`{ "id": 88, "itemNo": "LOST202610290001", "message": "登记成功..." }`

---

## 三、认证与个人中心

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 公开 | 账号密码登录（租户/平台） |
| POST | `/api/auth/wx-login` | 公开 | 微信 code 登录 |
| POST | `/api/auth/wx-bind` | 公开 | 账号密码 + 微信 code 绑定 |
| GET | `/api/auth/me` | 登录 | 当前用户、租户配置、菜单 |
| POST | `/api/auth/change-password` | 登录 | 修改密码 |
| POST | `/api/auth/logout` | 登录 | 退出（客户端删令牌） |

### 3.1 登录

```json
{
  "username": "demo_admin",
  "password": "Admin@123",
  "loginType": "tenant",
  "tenantCode": "DEMO"
}
```

- `loginType`：`tenant`（默认）或 `platform`。
- 平台账号可省略 `tenantCode`；租户账号同名时用 `tenantCode` 消歧。
- 租户停用/到期返回 `403`，数据保留。

成功响应：

```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1, "username": "demo_admin", "realName": "示范管理员",
      "roleCode": "TENANT_ADMIN", "userType": "STAFF",
      "tenantId": 1, "platform": false
    },
    "tenant": {
      "id": 1, "name": "示范中学", "code": "DEMO",
      "type": "SCHOOL", "status": "NORMAL", "expireTime": "2027-12-31 23:59:59"
    },
    "menus": [],
    "languages": []
  }
}
```

### 3.2 微信登录/绑定

```json
// POST /api/auth/wx-login
{ "code": "wx.login 返回的 code", "appType": "miniprogram" }
```

未绑定档案时返回 `{ "bound": false, "openid": "...", "unionid": "...", "message": "微信账号尚未绑定本校档案..." }`；已绑定返回与账号登录相同的 `token`、`user`、`tenant`、`menus`。

```json
// POST /api/auth/wx-bind
{
  "username": "demo_student",
  "password": "Admin@123",
  "code": "wx.login 返回的 code",
  "appType": "miniprogram"
}
```

`appType`：`miniprogram`（小程序）、`mp`（公众号）、`app`（独立 App）。微信开放平台未配置 UnionID 时，绑定会明确报错，需先完成 `wechat` 配置。

### 3.3 修改密码

```json
{ "oldPassword": "Admin@123", "newPassword": "NewPass@2026" }
```

新密码 8–32 位。修改成功写入审计；建议客户端强制重新登录。

---

## 四、通用 CRUD（78 资源）

平台为 78 张业务表提供统一 CRUD，避免为简单台账重复开发；复杂流程由专项接口承担。

基础路径：`/api/crud/{resource}`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/crud/{resource}` | 分页列表 |
| GET | `/api/crud/{resource}/all` | 不分页列表（最多返回业务表全部匹配） |
| GET | `/api/crud/{resource}/{id}` | 详情 |
| POST | `/api/crud/{resource}` | 新增 |
| PUT | `/api/crud/{resource}/{id}` | 修改 |
| DELETE | `/api/crud/{resource}/{id}` | 逻辑删除 |
| POST | `/api/crud/{resource}/batch-delete` | 批量逻辑删除，body 为 `[1,2,3]` |

### 4.1 列表参数

| 参数 | 说明 |
|---|---|
| `page` / `size` | 页码（默认 1）/ 每页条数（默认 10，最大 500） |
| `orderBy` | 排序，如 `createTime:desc,id:asc` |
| `keyword` | 按资源声明的搜索字段模糊匹配 |
| 其他字段 | 等值过滤；`字段Start`/`字段End` 为时间区间；逗号分隔值转为 `IN` |

示例：`GET /api/crud/visitor-appointment?page=1&size=10&status=APPROVED&visitTimeStart=2026-11-01&visitTimeEnd=2026-11-30`

### 4.2 资源清单

| 模块 | 资源名 |
|---|---|
| 平台/系统 | `tenant`、`platform-user`、`global-dict`、`platform-param`、`language`、`menu`、`role`、`role-menu`、`tenant-user`、`tenant-dict`、`tenant-config`、`audit-log`、`attachment`、`storage-config`、`wechat-config`、`message-template`、`message`、`message-log` |
| 门禁访客 | `visitor-appointment`、`visit-record`、`blacklist`、`key-person`、`temp-entry`、`late-return` |
| 车辆/车牌 | `vehicle`、`vehicle-appointment`、`vehicle-pass`、`violation`、`vehicle-log`、`parking-fee`、`plate-config`、`plate-camera`、`plate-auth-record`、`plate-log` |
| 治安事件 | `security-event`、`work-order`、`dispute`、`mediation`、`alarm-link` |
| 消防 | `fire-facility`、`fire-inspection`、`hazard`、`fire-drill`、`fire-training`、`power-check`、`fire-alarm` |
| 巡逻 | `patrol-route`、`patrol-point`、`patrol-record`、`patrol-task`、`dorm-check` |
| 失物 | `lost-item`、`lost-notice`、`lost-claim`、`lost-disposal` |
| 安全教育 | `safety-course`、`course-study`、`exam`、`exam-record`、`warning`、`notice`、`material` |
| 报备 | `report-apply`、`monitor-access` |
| 设备 | `device`、`device-repair`、`device-follow`、`device-inspection` |
| 应急 | `emergency-plan`、`emergency-supply`、`supply-record`、`emergency-contact`、`emergency-event` |
| 后勤 | `schedule`、`attendance`、`performance`、`feedback`、`monthly-report` |

### 4.3 CRUD 示例

```http
POST /api/crud/blacklist
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "张某",
  "idCard": "110101199001011234",
  "phone": "13800138000",
  "reason": "多次扰乱校园秩序",
  "status": "ACTIVE"
}
```

服务端自动补齐 `tenant_id`、`create_time`、`update_time`、`deleted`；租户用户不能写入其他租户；平台下钻只读。身份证/手机号等敏感字段由实体序列化器输出掩码。

---

## 五、看板与报表

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/dashboard` | `dashboard:read` | 租户看板 |
| GET | `/api/dashboard/platform` | `platform:read` | 全部学校看板（仅平台） |
| GET | `/api/reports/tenant` | `report:read` | 租户综合报表 |
| GET | `/api/reports/{module}?start=&end=` | `report:read` | 模块统计，`module` 可为 `visitor`/`vehicle`/`event`/`hazard`/`fire`/`patrol`/`lost`/`report`/`plate` 等 |
| GET | `/api/reports/export?module=&start=&end=` | `report:read` | 导出 Excel |

`GET /api/dashboard` 返回示例：

```json
{
  "code": 0,
  "data": {
    "tenant": { "id": 1, "name": "示范中学" },
    "visitor": { "today": 12, "pending": 3, "approved": 8, "total": 260 },
    "vehicle": { "inPark": 18, "todayIn": 42, "todayOut": 36 },
    "event": { "open": 2, "today": 4, "closed": 96 },
    "hazard": { "open": 3, "overdue": 1, "rectifyRate": 0.92 },
    "fire": { "facilities": 120, "intactRate": 0.98, "expiring": 2 },
    "patrol": { "todayTasks": 8, "finishRate": 0.88, "onTimeRate": 0.93 },
    "lost": { "unclaimed": 6, "claimRate": 0.75 },
    "alarm": { "today": 1, "open": 0 }
  }
}
```

平台看板 `data.schools` 为各校指标数组，可下钻只读。

---

## 六、门禁访客

基础路径：`/api/visitor`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/appointments` | `visitor:read` | 预约分页，支持 `status`、`keyword`、`start`、`end` |
| POST | `/appointments` | `visitor:write` | 创建预约（保安代录/后台录入） |
| POST | `/appointments/{id}/approve` | `visitor:approve` | 审批 |
| POST | `/appointments/{id}/cancel` | `visitor:write` | 取消 |
| GET | `/appointments/{id}/qr` | `visitor:read` | 通行码 Base64 + 有效期 |
| GET | `/qr?content=&size=` | `visitor:read` | 生成任意内容二维码 PNG |
| POST | `/check` | `visitor:write` | 门岗核验通行码 |
| GET | `/records` | `visitor:read` | 通行台账 |
| GET | `/stats?start=&end=` | `visitor:read` | 访客统计 |
| GET | `/export?status=` | `visitor:read` | 导出台账 Excel |

审批：

```json
{ "approved": true, "remark": "同意入校，请从南门进入" }
```

核验：

```json
{ "passCode": "VP202611010001", "gate": "南门", "method": "QR" }
```

成功返回通行记录；过期、已使用、黑名单、租户不匹配均返回明确错误并记审计。审批通过自动生成临时通行码，有效期由平台参数 `visitor_pass_hours` 控制。

---

## 七、车辆管理与车牌识别

### 7.1 车辆业务 `/api/vehicle`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/vehicles` | `vehicle:read` | 车辆分页 |
| POST | `/vehicles/{id}/issue` | `vehicle:write` | 固定车牌下发到全部相机 |
| POST | `/vehicles/{id}/remove` | `vehicle:write` | 注销车牌（全部相机） |
| GET | `/appointments` | `vehicle:read` | 外来车辆预约 |
| POST | `/appointments` | `vehicle:write` | 创建预约 |
| POST | `/appointments/{id}/approve` | `vehicle:write` | 审批并下发临时车牌 |
| POST | `/appointments/{id}/retry` | `vehicle:write` | 重新下发临时车牌 |
| GET | `/logs` | `vehicle:read` | 进出记录 |
| GET | `/in-park` | `vehicle:read` | 在场车辆 |
| POST | `/sync-logs` | `vehicle:write` | 从车牌平台同步进出日志 |
| GET | `/stats` | `vehicle:read` | 车辆统计 |

### 7.2 车牌识别配置与下发 `/api/plate`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/config` | `plate:read` | 获取配置（密钥仅回掩码） |
| PUT | `/config` | `plate:write` | 保存配置，`tested`+`mqttConnected` 同时为真才启用 |
| POST | `/test` | `plate:write` | 连接测试，调用厂商 `/api/health` |
| GET | `/cameras` | `plate:read` | 本地相机台账 |
| POST | `/cameras` | `plate:write` | 新增相机 |
| PUT | `/cameras/{id}` | `plate:write` | 修改相机 |
| DELETE | `/cameras/{id}` | `plate:write` | 删除相机 |
| POST | `/cameras/sync` | `plate:write` | 从厂商同步相机 |
| POST | `/auth/add-all` | `plate:write` | 固定车牌下发全部相机 |
| POST | `/auth/add` | `plate:write` | 固定车牌下发指定相机 |
| POST | `/auth/temp` | `plate:write` | 临时车牌下发 |
| POST | `/auth/del` | `plate:write` | 删除指定相机授权 |
| POST | `/auth/del-all` | `plate:write` | 删除全部相机授权 |
| GET | `/auth/query?plateNo=` | `plate:read` | 实时查询厂商授权 |
| GET | `/auth/records` | `plate:read` | 厂商授权记录对账 |
| GET | `/records` | `plate:read` | 本地下发台账 |
| GET | `/logs?plateNo=&cameraId=&inOut=&startTime=&endTime=` | `plate:read` | 厂商进出日志 |
| GET | `/logs/in-park` | `plate:read` | 厂商在场车辆 |
| POST | `/logs/sync` | `plate:write` | 同步进出日志到本地 |

配置保存：

```json
{
  "baseUrl": "https://plate.vendor.example.com",
  "apiKey": "vendor-api-key",
  "tested": true,
  "mqttConnected": true
}
```

- `baseUrl`、`apiKey` 按租户隔离；`apiKey` AES 加密存 `api_key_enc`，页面仅显示 `apiKeyMask`。
- 连接测试不通过或 `mqtt_connected=false` 时保存为 `DISABLED`，业务下发返回明确错误。
- 每次保存、测试、下发、删除、查询均写审计。

下发请求：

```json
// /auth/add-all、/auth/add
{ "plateNo": "京A12345", "cameraId": "cam-01" }

// /auth/temp
{
  "plateNo": "京B67890",
  "startTime": "2026-11-01 08:00:00",
  "endTime": "2026-11-01 20:00:00"
}
```

下发响应：

```json
{
  "total": 4,
  "success": 3,
  "failed": 1,
  "batchNo": "BATCH-1761700000000",
  "details": [
    { "cameraId": "cam-01", "cameraName": "南门入口", "status": "active", "msg": "ok" },
    { "cameraId": "cam-02", "cameraName": "北门入口", "status": "failed", "msg": "timeout" }
  ]
}
```

失败逐台落库为 `failed`，可在授权台账按 `status=failed` 检索并重试；批量操作有分布式锁防重复。

### 7.3 厂商接口契约（出站）

平台作为客户端调用车牌识别网关，请求头统一 `X-API-Key: <apiKey>`，超时 8 秒连接 / 10 秒读取。

| 方法 | 厂商路径 | 请求 | 关键响应 |
|---|---|---|---|
| GET | `/api/health` | - | `mqtt_connected` |
| GET | `/api/cameras` | - | `cameras[]`：`cam_id`、`name`、`campus`、`cam_type`、`remark`、`enabled` |
| POST | `/api/auth/add` | `plate_num`、`camera_id?` | `camera_id`、`name`、`status`、`msg` |
| POST | `/api/auth/add-all` | `plate_num` | `total`、`success`、`failed`、`results[]` |
| POST | `/api/auth/temp` | `plate_num`、`start_time`、`end_time` | 同上 |
| POST | `/api/auth/del` | `plate_num`、`camera_id?` | 同上 |
| POST | `/api/auth/del-all` | `plate_num` | 同上 |
| GET | `/api/auth/query?plate_num=` | - | 授权状态 |
| GET | `/api/auth/records` | - | 厂商侧授权记录 |
| GET | `/api/logs?plate_num=&camera_id=&in_out=&start_time=&end_time=` | - | 进出日志 |
| GET | `/api/logs/in-park` | - | 在场车辆 |

`status` 为 `active`/`ok`/`success` 视为成功，其余落 `failed`。厂商字段使用下划线，平台内部转换为驼峰。

---

## 八、治安事件与工单

基础路径：`/api/event`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/events` | `event:read` | 事件分页，支持 `status`、`urgency`、`eventType`、时间范围 |
| POST | `/report` | `event:write` | 事件上报（可带照片、定位） |
| POST | `/alarm` | `event:write` | 一键报警/求助 |
| POST | `/{id}/assign` | `event:write` | 分派保安/值班 |
| POST | `/{id}/handle` | `event:write` | 现场处置记录 |
| POST | `/{id}/close` | `event:write` | 结案归档 |
| GET | `/stats` | `event:read` | 事件统计 |
| GET | `/disputes` | `event:read` | 纠纷列表 |
| POST | `/disputes` | `event:write` | 登记纠纷 |
| POST | `/disputes/{id}/mediations` | `event:write` | 新增调解记录 |
| GET | `/alarms` | `event:read` | 报警联动记录 |
| POST | `/alarms` | `event:write` | 新增联动记录 |

上报：

```json
{
  "eventType": "THEFT",
  "location": "3 号教学楼东侧",
  "description": "学生书包丢失",
  "urgency": "HIGH",
  "photos": "[\"https://.../a.jpg\"]",
  "reporter": "李同学",
  "reporterPhone": "13800138000",
  "source": "APP"
}
```

服务端生成 `eventNo`，手机号落库掩码，重复提交由 Redis 防重键拦截。

---

## 九、消防与隐患

基础路径：`/api/fire`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/facilities` | `fire:read` | 设施台账 |
| POST | `/facilities` | `fire:write` | 新增设施，生成一物一码 |
| PUT | `/facilities/{id}` | `fire:write` | 修改设施 |
| GET | `/facilities/{id}/qr` | `fire:read` | 设施二维码 PNG |
| GET | `/inspections` | `fire:read` | 巡检记录 |
| POST | `/inspect` | `fire:write` | 扫码巡检；异常可自动转隐患 |
| GET | `/hazards` | `hazard:read` | 隐患列表 |
| POST | `/hazards` | `hazard:write` | 隐患上报 |
| POST | `/hazards/{id}/assign` | `hazard:write` | 分派责任人并设限期 |
| POST | `/hazards/{id}/rectify` | `hazard:write` | 提交整改方案/照片 |
| POST | `/hazards/{id}/accept` | `hazard:write` | 验收销号；不通过退回重改 |
| GET | `/stats` | `fire:read` | 消防统计 |
| GET | `/expiring` | `fire:read` | 临期设施/年检提醒 |

巡检：

```json
{
  "facilityNo": "FIRE-0001",
  "result": "ABNORMAL",
  "abnormalFlag": 1,
  "photos": "[\"https://.../b.jpg\"]",
  "remark": "压力表指针异常"
}
```

隐患整改：

```json
{ "rectifyPlan": "更换压力表并复检", "rectifyPhotos": "[\"https://.../c.jpg\"]" }
```

验收：

```json
{ "accepted": true, "acceptRemark": "已复检合格" }
```

重大隐患按平台参数自动升级预警；超期未闭环定时任务重复提醒并升级。

---

## 十、巡逻巡更

基础路径：`/api/patrol`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/routes` | `patrol:read` | 路线列表 |
| GET | `/points?routeId=` | `patrol:read` | 点位列表 |
| GET | `/points/{id}/qr` | `patrol:read` | 点位二维码 PNG |
| POST | `/checkin` | `patrol:write` | 扫码/GPS 打卡 |
| GET | `/records` | `patrol:read` | 巡逻记录 |
| GET | `/stats?start=&end=` | `patrol:read` | 完成率/按时率 |

打卡：

```json
{
  "pointId": 12,
  "routeId": 3,
  "method": "QR",
  "longitude": 116.397,
  "latitude": 39.908,
  "status": "NORMAL",
  "abnormalDesc": "",
  "photos": "[]",
  "remark": "正常"
}
```

App 弱网时本地入队，联网后按原打卡时间补传；服务端按点位时间窗判定迟到/漏点并生成提醒。

---

## 十一、失物招领

基础路径：`/api/lost`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/items` | `lost:read` | 失物台账 |
| POST | `/items` | `lost:write` | 捡拾登记 |
| POST | `/items/{id}/publish` | `lost:write` | 发布失物公告 |
| GET | `/notices` | `lost:read` | 公告列表 |
| GET | `/claims` | `lost:read` | 认领记录 |
| POST | `/claims` | `lost:write` | 认领登记（身份核对） |
| POST | `/claims/{id}/approve` | `lost:write` | 认领审批并通知 |
| GET | `/disposals` | `lost:read` | 处置申请 |
| POST | `/disposals` | `lost:write` | 未认领处置申请（归档/销毁） |
| POST | `/disposals/{id}/approve` | `lost:write` | 处置审批 |
| GET | `/stats` | `lost:read` | 认领率统计 |

失物登记自动生成 `itemNo`，并按描述/地点/分类计算匹配度推送给失主。

---

## 十二、安全教育

基础路径：`/api/education`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/exams` | `education:read` | 考试列表（不含答案） |
| POST | `/courses/{courseId}/study` | `education:write` | 记录学习进度 |
| POST | `/exams/{id}/submit` | `education:write` | 提交答卷，自动判分 |
| GET | `/records` | `education:read` | 全部学习/考试记录 |
| GET | `/my-records` | 登录 | 本人记录 |
| POST | `/warnings` | `education:write` | 发布安全预警/推送 |
| GET | `/stats` | `education:read` | 学习/考试统计 |

提交答卷：

```json
{ "answers": { "1": "A", "2": "C", "3": "B" } }
```

返回 `score`、`passed`、`correctCount`，并写 `exam_record`。

---

## 十三、报备与调监控

基础路径：`/api/report`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/applies` | `report:read` | 报备列表，`type` 支持 `ACTIVITY`/`CONSTRUCTION`/`HOTWORK`/`OUTING`/`CHEMICAL`/`BANNER` |
| POST | `/applies` | `report:write` | 提交报备 |
| POST | `/applies/{id}/approve` | `report:write` | 审批并生成电子回执 |
| GET | `/monitor-access` | `report:read` | 调监控申请 |
| POST | `/monitor-access` | `report:write` | 提交申请（时间/区域/事由） |
| POST | `/monitor-access/{id}/approve` | `report:write` | 分级审批，`stage`=`DEPT`/`LEADER` |
| POST | `/monitor-access/{id}/access` | `report:write` | 调取登记与结果反馈 |
| GET | `/stats` | `report:read` | 报备统计 |

调监控审批：

```json
{ "approved": true, "stage": "LEADER", "remark": "同意调取，仅限指定时段" }
```

调取登记：

```json
{ "accessRecord": "2026-11-01 10:00 保卫处监控室，民警王警官", "result": "已提供 10:00-10:30 录像" }
```

敏感操作额外鉴权并留痕，审计不可删除。

---

## 十四、设备、应急、后勤

### 14.1 设备 `/api/device`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/repairs` | `device:read` | 报修工单 |
| POST | `/repairs` | `device:write` | 提交报修 |
| POST | `/repairs/{id}/assign` | `device:write` | 分派维修 |
| POST | `/repairs/{id}/follow` | `device:write` | 维修跟进 |
| POST | `/repairs/{id}/finish` | `device:write` | 完工确认 |
| GET | `/stats` | `device:read` | 设备统计 |

设备台账通过 `/api/crud/device` 维护，巡检通过 `/api/crud/device-inspection`。

### 14.2 应急 `/api/emergency`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET/POST | `/plans` | `emergency:read/write` | 预案列表/新增 |
| PUT | `/plans/{id}` | `emergency:write` | 修改预案 |
| POST | `/plans/{id}/publish` | `emergency:write` | 发布预案 |
| GET/POST | `/supplies` | `emergency:read/write` | 物资列表/新增 |
| PUT | `/supplies/{id}` | `emergency:write` | 修改物资 |
| POST | `/supplies/{id}/borrow` | `emergency:write` | 领用 |
| POST | `/supplies/{id}/return` | `emergency:write` | 归还 |
| GET | `/supply-records` | `emergency:read` | 领用/归还记录 |
| GET/POST | `/contacts` | `emergency:read/write` | 通讯录 |
| PUT/DELETE | `/contacts/{id}` | `emergency:write` | 修改/删除联系人 |
| GET/POST | `/events` | `emergency:read/write` | 突发事件 |
| POST | `/events/{id}/respond` | `emergency:write` | 响应记录 |
| GET | `/stats` | `emergency:read` | 应急统计 |

领用/归还：

```json
{ "quantity": 2, "receiver": "王老师", "remark": "消防演练使用" }
```

### 14.3 后勤 `/api/logistics`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET/POST | `/schedules` | `logistics:read/write` | 排班列表/新增 |
| PUT/DELETE | `/schedules/{id}` | `logistics:write` | 修改/删除排班 |
| GET | `/attendance` | `logistics:read` | 考勤记录 |
| POST | `/attendance/check` | `logistics:write` | 考勤打卡 |
| GET | `/performance` | `logistics:read` | 绩效列表 |
| POST | `/performance` | `logistics:write` | 绩效登记 |
| GET | `/feedback` | `feedback:read` | 投诉/反馈 |
| POST | `/feedback` | `feedback:write` | 提交反馈 |
| POST | `/feedback/{id}/handle` | `feedback:write` | 处理反馈 |
| GET | `/monthly` | `logistics:read` | 月报列表 |
| POST | `/monthly/generate` | `logistics:write` | 生成月报 |
| GET | `/monthly/{month}/pdf` | `logistics:read` | 月报 PDF |
| GET | `/stats` | `logistics:read` | 后勤统计 |

---

## 十五、系统与平台

### 15.1 用户与角色 `/api/users`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `` | `system:read` | 用户分页 |
| GET | `/{id}` | `system:read` | 用户详情 |
| POST | `` | `system:write` | 新增用户（配额校验） |
| PUT | `/{id}` | `system:write` | 修改用户 |
| DELETE | `/{id}` | `system:write` | 逻辑删除 |
| POST | `/{id}/reset-password` | `system:write` | 重置密码 |
| GET/POST | `/roles` | `system:read/write` | 角色列表/新增 |
| PUT/DELETE | `/roles/{id}` | `system:write` | 修改/删除角色 |
| GET | `/roles/{id}/menus` | `system:read` | 角色菜单 |
| PUT | `/roles/{id}/menus` | `system:write` | 分配菜单 |
| GET | `/visitees` | `system:read` | 可被访人列表 |
| GET | `/export` | `system:read` | 导出用户 Excel |
| POST | `/import` | `system:write` | 导入用户 Excel |

### 15.2 平台 `/api/platform`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/tenants` | `tenant:read` | 租户分页 |
| GET | `/tenants/options` | `tenant:read` | 租户下拉 |
| POST | `/tenants` | `tenant:write` | 开通租户（初始化管理员/角色/菜单） |
| PUT | `/tenants/{id}` | `tenant:write` | 修改租户 |
| POST | `/tenants/{id}/status` | `tenant:write` | 启停 |
| POST | `/tenants/{id}/renew` | `tenant:write` | 续期 |
| GET | `/tenants/{id}` | `tenant:read` | 租户详情 |
| GET | `/dashboard` | `platform:read` | 平台看板 |
| GET | `/users` | `platform:read` | 平台用户 |
| POST | `/users` | `platform:write` | 新增平台用户 |
| GET | `/reports` | `platform:read` | 平台报表 |

启停：

```json
{ "status": "DISABLED", "reason": "合同到期" }
```

停用后该校全部用户不可登录，数据保留；操作写审计。

### 15.3 系统配置 `/api/system`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/params` | `param:read` | 平台参数 |
| PUT | `/params/{key}` | `param:write` | 修改参数 |
| GET/PUT | `/tenant-config` | `system:read/write` | 租户配置（密钥掩码） |
| GET | `/dicts?type=` | `dict:read` | 全局+租户字典 |
| POST/PUT/DELETE | `/dicts/global`、`/dicts/global/{id}` | `dict:write` | 全局字典维护（仅平台） |
| POST/PUT/DELETE | `/dicts/tenant`、`/dicts/tenant/{id}` | `dict:write` | 租户扩展字典 |
| GET/POST/PUT/DELETE | `/languages`、`/languages/{id}` | `i18n:read/write` | 语言启停与内容 |
| GET | `/menus`、`/menus/tree` | `menu:read` | 菜单列表/树 |
| POST/PUT/DELETE | `/menus`、`/menus/{id}` | `menu:write` | 菜单维护 |
| POST | `/menus/{id}/visible` | `menu:write` | 显示/隐藏 |
| GET/PUT | `/wechat/configs` | `wechat:read/write` | 微信配置（AppID/Secret/Token/EncodingAESKey，密钥掩码） |
| POST | `/wechat/menu/sync` | `wechat:write` | 同步公众号自定义菜单 |
| POST | `/wechat/materials/upload` | `wechat:write` | 上传永久素材（multipart：`file`、`type=image/voice/video/thumb`、`appType=MP`，≤10MB） |
| GET | `/wechat/materials?type=&offset=&count=&appType=` | `wechat:read` | 永久素材列表（count 上限 20） |
| DELETE | `/wechat/materials/{mediaId}?appType=` | `wechat:write` | 删除永久素材 |
| GET | `/wechat/tags?appType=` | `wechat:read` | 粉丝标签列表 |
| POST | `/wechat/tags` | `wechat:write` | 创建标签，body `{name, appType}` |
| POST | `/wechat/tags/fans` | `wechat:write` | 批量打标签，body `{tagId, openids[], appType}` |
| POST | `/wechat/mass/send` | `wechat:write` | 按标签群发，body `{tagId?, msgType: text/mpnews, content, appType}` |
| POST | `/wechat/subscribe/send` | `wechat:write` | 小程序订阅消息，body `{openid, templateId, page, data}` |
| GET/POST/PUT/DELETE | `/message-templates`... | `wechat:read/write` | 模板/订阅消息模板 |
| GET | `/messages` | `system:read` | 站内消息 |
| POST | `/messages/{id}/read` | `system:write` | 标记已读 |
| POST | `/messages/send` | `system:write` | 发送消息（限流+日志+失败重试） |
| GET | `/audit` | `audit:read` | 审计日志 |
| GET | `/audit/export` | `audit:read` | 导出审计 |
| GET | `/storage`、`/storage/drivers` | `storage:read` | 存储配置/驱动能力 |
| POST | `/storage/test` | `storage:write` | 连接测试 |
| POST | `/storage/save` | `storage:write` | 保存配置（密钥加密） |
| POST | `/upload` | `system:write` | 上传文件 |
| GET | `/attachments` | `system:read` | 附件列表 |
| GET | `/health` | 登录 | 依赖健康检查 |

租户配置可保存学校名称、Logo、值班电话、公众号/小程序、微信支付、对象存储、通知开关等；密钥字段 AES 加密且返回掩码，保存实时生效。

### 15.4 打印 `/api/print`

| 方法 | 路径 | 权限 | 输出 |
|---|---|---|---|
| GET | `/visitor/{id}/pass` | `visitor:read` | 访客通行码 PDF |
| GET | `/report/{id}` | `report:read` | 报备审批单 PDF |
| GET | `/hazard/{id}` | `hazard:read` | 隐患整改通知单 PDF |
| GET | `/lost/{id}/label` | `lost:read` | 失物标签 PDF |
| GET | `/monthly/{month}` | `report:read` | 月报 PDF |

PDF 中文字体由 `BWC_PDF_FONT` 指定，默认 `/usr/share/fonts/chinese/stsong.ttf`；缺失时返回明确错误，避免输出乱码。

### 15.5 微信回调 `/api/wechat/callback`

| 方法 | 说明 |
|---|---|
| GET | 微信服务器校验，校验 `signature`/`timestamp`/`nonce`/`echostr` |
| POST | 接收关注/取关/文本消息，XML 响应 |

回调地址需在公众号后台配置为 `https://wx-bwc-api.greathiit.com/api/wechat/callback`，Token 与后台「微信配置」一致。未配置时返回明确错误，不影响其他业务。

---

## 十六、客户端接入建议

### 16.1 小程序

1. `wx.login` 获取 code。
2. 优先 `POST /api/auth/wx-bind`（首次），之后 `POST /api/auth/wx-login`。
3. 令牌存 `wx.setStorageSync`，请求头 `Authorization`。
4. 租户编码存本地，公开接口路径使用 `/api/public/tenant/{code}/...`。
5. 合法域名仅配置 `https://wx-bwc-api.greathiit.com`。

### 16.2 H5

- 公开页直接调用 `/api/public/tenant/{code}/...`，无需令牌。
- 分享页带 `source=wechat-share` 与业务 ID，落地页调用 `/share/{type}/{id}`。
- 身份证号仅提交、不回显完整值，页面使用掩码。

### 16.3 App

- 登录后缓存令牌与用户信息；`401` 统一清理并跳转登录。
- 巡逻/巡检弱网时写入本地队列（SharedPreferences），恢复网络后按原时间补传。
- `X-Tenant-Id` 仅平台端使用，普通 App 不发送。

### 16.4 平台后台

- 平台账号查询自动跨租户；下钻时设置 `X-Tenant-Id` 并展示只读标识。
- 所有写操作在只读模式被拒绝，前端应隐藏/禁用写按钮。

---

## 十七、版本与兼容

- 当前版本 `v1`，路径不携带版本号；破坏性变更通过新路径或新字段兼容。
- 新增响应字段不影响旧客户端；删除/重命名字段需评估并提前公告。
- 第三方未配置时：微信相关返回明确错误；车牌识别返回 `400/502`；消息推送降级为站内消息；不影响主业务闭环。

---

## 十八、快速自检

```bash
# 健康检查
curl https://wx-bwc-api.greathiit.com/api/public/health

# 登录
curl -X POST https://wx-bwc-api.greathiit.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_admin","password":"Admin@123","tenantCode":"DEMO"}'

# 带令牌查询
curl 'https://wx-bwc-api.greathiit.com/api/visitor/appointments?page=1&size=10' \
  -H 'Authorization: Bearer <token>'

# 平台下钻只读
curl 'https://wx-bwc-api.greathiit.com/api/dashboard' \
  -H 'Authorization: Bearer <platform-token>' \
  -H 'X-Tenant-Id: 1'
```

> 本文档中的示例密钥、地址均为占位值，生产环境请使用真实配置并妥善保管。
