#!/usr/bin/env python3
"""Generate admin-web/src/config/resources.ts from backend @CrudResource entities.

Keeps the generic CRUD UI in sync with Java entities and DB schema.
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENTITY_DIR = ROOT / "backend/src/main/java/com/greathiit/bwc/module"

TITLES = {
    "visitor-appointment": "访客预约", "visit-record": "通行记录", "temp-entry": "临时入校",
    "blacklist": "黑名单", "key-person": "重点人员", "late-return": "晚归登记",
    "vehicle": "车辆备案", "vehicle-appointment": "车辆预约", "violation": "违停上报",
    "vehicle-log": "车辆进出记录", "parking-fee": "停车收费", "plate-config": "车牌识别配置",
    "plate-camera": "车牌识别相机", "plate-auth-record": "车牌授权台账", "plate-log": "相机进出日志",
    "vehicle-pass": "临时放行",
    "security-event": "治安事件", "work-order": "事件工单", "dispute": "纠纷登记",
    "mediation": "调解记录", "alarm-link": "报警联动",
    "fire-facility": "消防设施台账", "fire-inspection": "消防巡检", "hazard": "隐患整改",
    "fire-drill": "消防演练", "fire-training": "消防培训", "power-check": "违规用电核查",
    "fire-alarm": "消防报警",
    "patrol-route": "巡逻路线", "patrol-point": "巡逻点位", "patrol-record": "巡逻记录",
    "patrol-task": "巡逻任务", "dorm-check": "查寝巡查",
    "lost-item": "失物登记", "lost-notice": "失物公告", "lost-claim": "认领登记",
    "lost-disposal": "处置审批",
    "safety-course": "安全课程", "course-study": "学习记录", "exam": "安全考试",
    "exam-record": "考试成绩", "warning": "安全预警", "notice": "通知公告", "material": "宣传素材",
    "report-apply": "安全报备", "monitor-access": "调取监控申请",
    "device": "设备台账", "device-repair": "设备报修", "device-follow": "维修跟进",
    "device-inspection": "设备巡检",
    "emergency-plan": "应急预案", "emergency-supply": "应急物资", "supply-record": "物资领用记录",
    "emergency-contact": "应急通讯录", "emergency-event": "突发事件",
    "schedule": "保安排班", "attendance": "考勤记录", "performance": "绩效管理",
    "feedback": "投诉反馈", "monthly-report": "月报季报",
    "tenant": "租户管理", "platform-user": "平台账号", "tenant-user": "用户管理",
    "role": "角色权限", "role-menu": "角色菜单", "tenant-dict": "租户字典",
    "global-dict": "全局字典", "platform-param": "平台参数", "language": "多语言",
    "menu": "菜单配置", "storage-config": "对象存储", "wechat-config": "微信配置",
    "message-template": "消息模板", "message": "站内消息", "message-log": "消息日志",
    "audit-log": "审计日志", "attachment": "附件", "tenant-config": "租户配置",
}

FIELD_LABELS = {
    "visitorName": "访客姓名", "idCardMask": "身份证号", "phoneMask": "手机号", "reason": "来访事由",
    "visitTime": "来访时间", "leaveTime": "离校时间", "accompanyCount": "随行人数", "plateNo": "车牌号",
    "visitUserId": "被访人ID", "visitUserName": "被访人", "status": "状态", "approveBy": "审批人",
    "approveTime": "审批时间", "approveRemark": "审批意见", "passCode": "通行码",
    "passExpireTime": "通行码有效期", "source": "来源", "remark": "备注", "appointmentId": "预约单",
    "visitorId": "访客", "inTime": "入校时间", "outTime": "离校时间", "gate": "校门", "operator": "操作人",
    "method": "方式", "name": "名称", "level": "级别", "category": "分类", "inOut": "进出方向",
    "triggerTime": "触发时间", "cameraId": "相机ID", "cameraName": "相机名称", "isWhitelist": "白名单",
    "imageUrl": "抓拍图", "ownerName": "车主", "ownerId": "车主ID", "ownerPhoneMask": "车主手机号",
    "vehicleType": "车辆类型", "validStart": "有效期起", "validEnd": "有效期止", "authStatus": "授权状态",
    "authTime": "授权时间", "parkingNo": "车位号", "violateTime": "违停时间", "handleStatus": "处理状态",
    "handleResult": "处理结果", "fee": "费用", "payStatus": "支付状态", "payTime": "支付时间",
    "driver": "驾驶员", "startTime": "开始时间", "endTime": "结束时间", "plateType": "车牌类型",
    "action": "动作", "message": "消息", "batchNo": "批次号", "camId": "相机编号", "campus": "校区",
    "camType": "相机类型", "enabled": "启用", "syncStatus": "同步状态", "syncTime": "同步时间",
    "configJson": "接口配置", "baseUrl": "BASE 地址", "apiKeyMask": "API 密钥", "lastTestTime": "最近测试",
    "testResult": "测试结果", "eventNo": "事件编号", "eventType": "事件类型", "location": "地点",
    "description": "描述", "urgency": "紧急度", "photos": "照片", "reporter": "上报人",
    "reporterPhoneMask": "上报人手机号", "assignTo": "责任人ID", "assignToName": "责任人",
    "assignTime": "分派时间", "handleResultText": "处置结果", "handlePhotos": "处置照片",
    "closeTime": "结案时间", "orderNo": "工单编号", "eventId": "事件", "title": "标题",
    "deadline": "期限", "finishTime": "完成时间", "result": "结果", "disputeNo": "纠纷编号",
    "parties": "当事双方", "content": "内容", "mediator": "调解人", "mediationTime": "调解时间",
    "alarmType": "报警类型", "alarmTime": "报警时间", "linkEventId": "关联事件", "linkTime": "联动时间",
    "facilityNo": "设施编号", "type": "类型", "qrCode": "二维码", "productionDate": "生产日期",
    "expireDate": "有效期至", "lastCheckDate": "上次检查", "nextCheckDate": "下次检查",
    "responsible": "责任人", "facilityId": "设施ID", "inspector": "巡检人", "inspectTime": "巡检时间",
    "resultText": "巡检结果", "abnormalFlag": "异常标记", "hazardId": "隐患", "hazardNo": "隐患编号",
    "hazardLevel": "隐患等级", "reportTime": "上报时间", "rectifyPlan": "整改方案",
    "rectifyPhotos": "整改照片", "rectifyTime": "整改时间", "acceptBy": "验收人",
    "acceptTime": "验收时间", "acceptRemark": "验收意见", "drillTime": "演练时间",
    "participants": "参与人数", "summary": "总结", "trainer": "讲师", "trainTime": "培训时间",
    "contentText": "培训内容", "checkTime": "检查时间", "dorm": "宿舍", "violationDesc": "违规描述",
    "routeId": "路线ID", "pointId": "点位ID", "routeName": "路线名称", "pointName": "点位名称",
    "guardId": "保安ID", "guardName": "保安姓名", "seq": "顺序", "timeWindow": "时间窗",
    "longitude": "经度", "latitude": "纬度", "abnormalDesc": "异常描述", "planDate": "计划日期",
    "itemNo": "失物编号", "foundTime": "拾获时间", "finder": "拾获人", "storageLocation": "保管位置",
    "itemId": "失物", "claimant": "认领人", "claimTime": "认领时间", "verifyBy": "核验人",
    "disposalType": "处置方式", "disposalTime": "处置时间", "approver": "审批人",
    "courseId": "课程ID", "courseName": "课程名称", "duration": "时长", "cover": "封面",
    "videoUrl": "视频", "studyTime": "学习时间", "progress": "进度", "examId": "考试ID",
    "examName": "考试名称", "score": "分数", "passScore": "及格分", "questionsJson": "试题",
    "answerJson": "答案", "submitTime": "提交时间", "warningLevel": "预警级别",
    "publishTime": "发布时间", "publisher": "发布人", "views": "浏览量", "materialType": "素材类型",
    "url": "链接", "fileUrl": "文件", "reportNo": "报备编号", "applicant": "申请人",
    "applicantPhone": "申请人电话", "safetyPlan": "安全方案", "attachment": "附件",
    "receiptNo": "电子回执", "applyNo": "申请编号", "area": "区域", "reasonText": "事由",
    "approver1": "初审人", "approve1Time": "初审时间", "approver2": "终审人",
    "approve2Time": "终审时间", "accessRecord": "调取登记", "feedback": "反馈",
    "deviceNo": "设备编号", "vendor": "厂商", "model": "型号", "installDate": "安装日期",
    "deviceId": "设备", "deviceName": "设备名称", "faultDesc": "故障描述", "repairNo": "报修编号",
    "followTime": "跟进时间", "followContent": "跟进内容", "follower": "跟进人",
    "inspectionTime": "巡检时间", "inspectionResult": "巡检结果", "version": "版本",
    "quantity": "数量", "unit": "单位", "supplyId": "物资", "userName": "使用人",
    "recordTime": "记录时间", "dept": "部门", "duty": "职责", "phone": "电话", "sort": "排序",
    "occurTime": "发生时间", "response": "响应记录", "shiftDate": "排班日期", "shiftType": "班次",
    "checkTimeText": "打卡时间", "month": "月份", "itemsJson": "绩效项", "contact": "联系方式",
    "handler": "处理人", "handleTime": "处理时间", "configKey": "配置键", "configValue": "配置值",
    "encrypted": "加密", "code": "编码", "openTime": "开通时间", "expireTime": "到期时间",
    "userQuota": "用户配额", "username": "用户名", "realName": "姓名", "roleId": "角色ID",
    "roleCode": "角色编码", "userType": "用户类型", "openid": "OpenID", "unionid": "UnionID",
    "lastLoginTime": "最近登录", "roleName": "角色名称", "dataScope": "数据范围", "menuId": "菜单ID",
    "dictType": "字典类型", "dictLabel": "字典标签", "dictValue": "字典值", "langCode": "语言编码",
    "langName": "语言名称", "platform": "端", "parentId": "上级ID", "nameEn": "英文名称",
    "path": "路径", "icon": "图标", "menuType": "菜单类型", "permission": "权限标识",
    "visible": "可见", "driver": "驱动", "appType": "应用类型", "appId": "AppID",
    "appSecretMask": "AppSecret", "token": "Token", "aesKeyEnc": "EncodingAESKey",
    "serverUrl": "服务器URL", "authDomain": "网页授权域名", "jsDomain": "JS安全域名",
    "bizType": "业务类型", "channel": "通道", "templateId": "模板ID", "fieldsJson": "模板字段",
    "userId": "用户ID", "bizId": "业务ID", "readFlag": "已读", "error": "错误",
    "toUser": "接收人", "module": "模块", "target": "对象", "detail": "详情", "ip": "IP",
    "userAgent": "User-Agent", "fileName": "文件名", "size": "大小", "contentType": "文件类型",
}

OPTIONS = {
    "status": [("正常", "NORMAL"), ("停用", "DISABLED"), ("待处理", "PENDING"), ("已通过", "APPROVED"),
               ("已驳回", "REJECTED"), ("处理中", "HANDLING"), ("已完成", "DONE"), ("已关闭", "CLOSED"),
               ("已过期", "EXPIRED"), ("待验收", "PENDING_ACCEPT"), ("整改中", "RECTIFYING"),
               ("已分派", "ASSIGNED"), ("已上报", "REPORTED"), ("逾期", "OVERDUE"), ("已认领", "CLAIMED"),
               ("已发布", "PUBLISHED"), ("待领取", "FOUND"), ("已作废", "CANCELLED")],
    "shiftType": [("白班", "DAY"), ("夜班", "NIGHT")],
    "plateType": [("固定车牌", "FIXED"), ("临时车牌", "TEMP")],
    "channel": [("公众号", "MP"), ("小程序", "MINI"), ("站内", "STATION")],
    "appType": [("公众号", "MP"), ("小程序", "MINI"), ("开放平台", "OPEN")],
    "menuType": [("菜单", "MENU"), ("按钮", "BUTTON")],
    "urgency": [("普通", "NORMAL"), ("紧急", "URGENT"), ("特急", "CRITICAL")],
    "hazardLevel": [("一般", "GENERAL"), ("较大", "MAJOR"), ("重大", "CRITICAL")],
    "eventType": [("失窃", "THEFT"), ("纠纷", "DISPUTE"), ("求助", "HELP"), ("失物", "LOST"), ("治安", "SECURITY")],
    "dataScope": [("本租户", "TENANT"), ("本人", "SELF"), ("只读", "TENANT_READ")],
    "userType": [("管理员", "ADMIN"), ("队长", "CAPTAIN"), ("保安", "GUARD"), ("教职工", "STAFF"),
                 ("学生", "STUDENT"), ("校领导", "LEADER"), ("被访人", "VISITEE")],
    "type": [("投诉", "COMPLAINT"), ("建议", "SUGGESTION"), ("其他", "OTHER")],
    "result": [("正常", "NORMAL"), ("异常", "ABNORMAL")],
    "warningLevel": [("提示", "INFO"), ("警告", "WARN"), ("严重", "CRITICAL")],
    "disposalType": [("归档", "ARCHIVE"), ("销毁", "DESTROY")],
}

MENU_PATHS = {
    "/visitor/appointments": "visitor-appointment", "/visitor/records": "visit-record",
    "/visitor/temp": "temp-entry", "/visitor/blacklist": "blacklist",
    "/visitor/key-persons": "key-person", "/visitor/late-return": "late-return",
    "/vehicle/vehicles": "vehicle", "/vehicle/appointments": "vehicle-appointment",
    "/vehicle/violations": "violation", "/vehicle/logs": "vehicle-log", "/vehicle/fees": "parking-fee",
    "/vehicle/plate-config": "plate-config", "/vehicle/plate-cameras": "plate-camera",
    "/vehicle/plate-records": "plate-auth-record",
    "/event/events": "security-event", "/event/disputes": "dispute", "/event/alarms": "alarm-link",
    "/fire/facilities": "fire-facility", "/fire/inspections": "fire-inspection", "/fire/hazards": "hazard",
    "/fire/drills": "fire-drill", "/fire/trainings": "fire-training", "/fire/power-checks": "power-check",
    "/fire/alarms": "fire-alarm",
    "/patrol/routes": "patrol-route", "/patrol/points": "patrol-point", "/patrol/records": "patrol-record",
    "/patrol/dorm-checks": "dorm-check",
    "/lost/items": "lost-item", "/lost/notices": "lost-notice", "/lost/claims": "lost-claim",
    "/lost/disposals": "lost-disposal",
    "/education/courses": "safety-course", "/education/exams": "exam", "/education/warnings": "warning",
    "/education/notices": "notice", "/education/materials": "material",
    "/report/applies": "report-apply", "/report/monitor-access": "monitor-access",
    "/device/devices": "device", "/device/repairs": "device-repair", "/device/inspections": "device-inspection",
    "/emergency/plans": "emergency-plan", "/emergency/supplies": "emergency-supply",
    "/emergency/contacts": "emergency-contact", "/emergency/events": "emergency-event",
    "/logistics/schedules": "schedule", "/logistics/attendance": "attendance",
    "/logistics/performance": "performance", "/logistics/feedback": "feedback",
    "/system/users": "tenant-user", "/system/roles": "role", "/system/dicts": "global-dict",
    "/system/params": "platform-param", "/system/menus": "menu", "/system/i18n": "language",
    "/system/storage": "storage-config", "/system/wechat": "wechat-config",
    "/system/message-templates": "message-template", "/system/audit": "audit-log",
    "/platform/tenants": "tenant",
}

SPECIAL = {
    "/dashboard": "Dashboard", "/platform/dashboard": "PlatformDashboard",
    "/visitor/stats": "VisitorStats", "/patrol/stats": "PatrolStats",
    "/report-center/tenant": "TenantReports", "/report-center/platform": "PlatformReports",
    "/report-center/monthly": "MonthlyReports", "/system/params": "PlatformParams",
    "/system/tenant-config": "TenantConfig", "/system/dicts": "Dictionaries",
    "/system/menus": "Menus", "/system/i18n": "Languages", "/system/storage": "StorageConfig",
    "/system/wechat": "WechatConfig", "/system/message-templates": "MessageTemplates",
}


def js(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "\\'") + "'"


def main():
    resources = {}
    for path in ENTITY_DIR.rglob("entity/*.java"):
        text = path.read_text(encoding="utf-8")
        match = re.search(r'@CrudResource\(value = "([^"]+)", module = "([^"]+)"', text)
        if not match:
            continue
        key, module = match.groups()
        fields = []
        for typ, name in re.findall(r"private\s+([\w<>]+)\s+(\w+);", text):
            if name in {"id", "tenantId", "createTime", "updateTime", "deleted"}:
                continue
            ftype = "text"
            if typ in ("Integer", "Long", "BigDecimal"):
                ftype = "number"
            elif typ == "LocalDateTime":
                ftype = "datetime"
            elif typ == "LocalDate":
                ftype = "date"
            elif typ == "Boolean":
                ftype = "switch"
            else:
                low = name.lower()
                if any(k in low for k in ("photos", "image", "avatar", "logo", "cover", "attachment")):
                    ftype = "image"
                elif any(k in low for k in ("content", "description", "remark", "response", "plan",
                                            "result", "summary", "json", "safety")):
                    ftype = "textarea"
            label = FIELD_LABELS.get(name, name)
            options = OPTIONS.get(name)
            fields.append({"prop": name, "label": label, "type": ftype,
                           "options": options})
        resources[key] = {"key": key, "module": module,
                          "title": TITLES.get(key, key), "fields": fields}

    lines = ["/* eslint-disable */",
             "// AUTO-GENERATED by scripts/gen_admin_resources.py. Do not edit manually.",
             "export interface FieldOption { label: string; value: string }",
             "export interface FieldDef { prop: string; label: string; type: string; options?: FieldOption[] }",
             "export interface ResourceDef { key: string; module: string; title: string; fields: FieldDef[] }",
             "", "export const resources: Record<string, ResourceDef> = {"]
    for key in sorted(resources):
        item = resources[key]
        lines.append(f"  {js(key)}: {{")
        lines.append(f"    key: {js(key)}, module: {js(item['module'])}, title: {js(item['title'])},")
        lines.append("    fields: [")
        for field in item["fields"]:
            extra = ""
            if field["options"]:
                opts = ", ".join(f"{{ label: {js(l)}, value: {js(v)} }}" for l, v in field["options"])
                extra = f", options: [{opts}]"
            lines.append(f"      {{ prop: {js(field['prop'])}, label: {js(field['label'])}, "
                         f"type: {js(field['type'])}{extra} }},")
        lines.append("    ]")
        lines.append("  },")
    lines.append("}")
    lines.append("")
    lines.append("export const menuResource: Record<string, string> = {")
    for path, key in sorted(MENU_PATHS.items()):
        if key in resources:
            lines.append(f"  {js(path)}: {js(key)},")
    lines.append("}")
    lines.append("")
    lines.append("export const specialViews: Record<string, string> = {")
    for path, view in sorted(SPECIAL.items()):
        lines.append(f"  {js(path)}: {js(view)},")
    lines.append("}")
    lines.append("")

    target = ROOT / "admin-web/src/config/resources.ts"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text("\n".join(lines), encoding="utf-8")
    print(f"generated {target} with {len(resources)} resources")


if __name__ == "__main__":
    main()
