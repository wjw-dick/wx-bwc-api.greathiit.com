# 校园保卫 App（Flutter）

保安一线端 + 管理端 + 校领导只读看板。

## 功能

- 账号密码登录，支持租户编码；预留微信 UnionID 绑定能力。
- 校领导看板：今日访客、待审预约、未闭环隐患、巡逻打卡、消防告警等。
- 巡逻打卡：路线/点位选择、GPS 定位、扫码（mobile_scanner）、异常上报。
- 消防巡检：设施扫码/选择、巡检结果、隐患上报（拍照）。
- 事件接警与处置：事件列表、上报、分派、处理、结案。
- 管理端：访客/车辆/报备/失物认领审批、消息中心、修改密码。
- 弱网离线：巡逻/消防打卡失败自动进入本地队列，联网后一键补传。
- 多语言：中文 / English 实时切换。
- 导航可视化：首页快捷入口读取后台 `platform=app` 菜单配置。

## 运行

```bash
cd app
flutter pub get
flutter create --platforms=android,ios .   # 首次生成原生工程（不会覆盖 lib）
flutter run --dart-define=BWC_BASE_URL=https://wx-bwc-api.greathiit.com/api
```

默认后端地址：`https://wx-bwc-api.greathiit.com/api`，默认学校编码：`DEMO`。

## 说明

- 华为/小米等应用市场发布需自行配置签名、隐私政策与权限说明。
- 扫码、定位、相机权限需在首次使用时授权。
- 微信 UnionID 绑定需接入微信开放平台 SDK，当前以账号密码登录为主，绑定接口与小程序/公众号一致（`/auth/wx-bind`）。
