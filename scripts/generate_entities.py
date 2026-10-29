#!/usr/bin/env python3
"""从 sql/schema.sql 生成 MyBatis-Plus 实体与 Mapper，保证表结构/代码同源。"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCHEMA = os.path.join(ROOT, "sql", "schema.sql")
BASE_PKG = "com.greathiit.bwc.module"
BASE_DIR = os.path.join(ROOT, "backend", "src", "main", "java", "com", "greathiit", "bwc", "module")

MODULE_MAP = {
    "sys_tenant": "system", "sys_platform_user": "system", "sys_global_dict": "system",
    "sys_platform_param": "system", "sys_language": "system", "sys_menu": "system",
    "sys_role": "system", "sys_role_menu": "system", "sys_tenant_user": "system",
    "sys_tenant_dict": "system", "sys_tenant_config": "system", "sys_audit_log": "system",
    "sys_attachment": "system", "sys_storage_config": "system", "sys_wechat_config": "system",
    "sys_message_template": "system", "sys_message": "system", "sys_message_log": "system",
    "bwc_visitor_appointment": "visitor", "bwc_visit_record": "visitor", "bwc_blacklist": "visitor",
    "bwc_key_person": "visitor", "bwc_temp_entry": "visitor", "bwc_late_return": "visitor",
    "bwc_vehicle": "vehicle", "bwc_vehicle_appointment": "vehicle", "bwc_vehicle_pass": "vehicle",
    "bwc_violation": "vehicle", "bwc_vehicle_log": "vehicle", "bwc_parking_fee": "vehicle",
    "bwc_plate_config": "vehicle", "bwc_plate_camera": "vehicle",
    "bwc_plate_auth_record": "vehicle", "bwc_plate_log": "vehicle",
    "bwc_security_event": "event", "bwc_work_order": "event", "bwc_dispute": "event",
    "bwc_mediation": "event", "bwc_alarm_link": "event",
    "bwc_fire_facility": "fire", "bwc_fire_inspection": "fire", "bwc_hazard": "fire",
    "bwc_fire_drill": "fire", "bwc_fire_training": "fire", "bwc_power_check": "fire",
    "bwc_fire_alarm": "fire",
    "bwc_patrol_route": "patrol", "bwc_patrol_point": "patrol", "bwc_patrol_record": "patrol",
    "bwc_patrol_task": "patrol", "bwc_dorm_check": "patrol",
    "bwc_lost_item": "lost", "bwc_lost_notice": "lost", "bwc_lost_claim": "lost",
    "bwc_lost_disposal": "lost",
    "bwc_safety_course": "education", "bwc_course_study": "education", "bwc_exam": "education",
    "bwc_exam_record": "education", "bwc_warning": "education", "bwc_notice": "education",
    "bwc_material": "education",
    "bwc_report_apply": "report", "bwc_monitor_access": "report",
    "bwc_device": "device", "bwc_device_repair": "device", "bwc_device_follow": "device",
    "bwc_device_inspection": "device",
    "bwc_emergency_plan": "emergency", "bwc_emergency_supply": "emergency",
    "bwc_supply_record": "emergency", "bwc_emergency_contact": "emergency",
    "bwc_emergency_event": "emergency",
    "bwc_schedule": "logistics", "bwc_attendance": "logistics", "bwc_performance": "logistics",
    "bwc_feedback": "logistics", "bwc_monthly_report": "logistics",
}

PLATFORM_TABLES = {"sys_tenant", "sys_platform_user", "sys_global_dict", "sys_platform_param",
                   "sys_language", "sys_menu"}
MODULE_OF_PLATFORM = {
    "sys_tenant": "tenant", "sys_platform_user": "platform", "sys_global_dict": "dict",
    "sys_platform_param": "param", "sys_language": "language", "sys_menu": "menu",
}

SKIP = {"id", "tenant_id", "create_time", "update_time", "deleted"}

TYPE_MAP = {
    "bigint": "Long", "int": "Integer", "tinyint": "Integer", "smallint": "Integer",
    "decimal": "BigDecimal", "datetime": "LocalDateTime", "date": "LocalDate",
    "timestamp": "LocalDateTime", "varchar": "String", "char": "String",
    "text": "String", "longtext": "String", "mediumtext": "String", "json": "String",
}


def camel(name):
    return "".join(part.capitalize() for part in name.split("_"))


def field_camel(name):
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def parse_tables(sql):
    tables = []
    for m in re.finditer(r"CREATE TABLE `(\w+)` \((.*?)\n\) ENGINE", sql, re.S):
        name, body = m.group(1), m.group(2)
        cols = []
        for line in body.splitlines():
            line = line.strip()
            if not line.startswith("`"):
                continue
            cm = re.match(r"`(\w+)`\s+([\w()]+)", line)
            if not cm:
                continue
            col, sqltype = cm.group(1), cm.group(2).lower()
            if col in SKIP or col in ("id",):
                continue
            base = re.sub(r"\(.*\)", "", sqltype)
            jtype = TYPE_MAP.get(base)
            if not jtype:
                continue
            cols.append((field_camel(col), jtype))
        tables.append((name, cols))
    return tables


def java_type_list(cols):
    return {t for _, t in cols}


def main():
    sql = open(SCHEMA, encoding="utf-8").read()
    index = 0
    for table, cols in parse_tables(sql):
        module = MODULE_MAP.get(table)
        if not module:
            print("skip unknown table", table, file=sys.stderr)
            continue
        cls = camel(table)
        resource = table.replace("sys_", "").replace("bwc_", "").replace("_", "-")
        crud_module = MODULE_OF_PLATFORM.get(table, module)
        if table in PLATFORM_TABLES:
            crud_module = MODULE_OF_PLATFORM[table]
        search = []
        colnames = {c for c, _ in cols}
        for candidate in ("name", "title", "plateNo", "username", "realName", "facilityNo",
                          "eventNo", "hazardNo", "orderNo", "repairNo", "reportNo", "itemNo",
                          "visitorName", "deviceName", "guardName"):
            if candidate in colnames:
                search.append(candidate)
        pkg = f"{BASE_PKG}.{module}"
        entity_dir = os.path.join(BASE_DIR, module, "entity")
        mapper_dir = os.path.join(BASE_DIR, module, "mapper")
        os.makedirs(entity_dir, exist_ok=True)
        os.makedirs(mapper_dir, exist_ok=True)

        imports = [
            "com.baomidou.mybatisplus.annotation.TableName",
            "com.greathiit.bwc.common.BaseEntity",
            "com.greathiit.bwc.common.CrudResource",
            "lombok.Data",
            "lombok.EqualsAndHashCode",
        ]
        types = java_type_list(cols)
        if "LocalDateTime" in types:
            imports.append("java.time.LocalDateTime")
        if "LocalDate" in types:
            imports.append("java.time.LocalDate")
        if "BigDecimal" in types:
            imports.append("java.math.BigDecimal")

        search_expr = "{" + ", ".join(f'"{s}"' for s in search) + "}"
        lines = [f"package {pkg}.entity;", ""]
        lines += [f"import {i};" for i in imports]
        lines += ["", "/**", f" * {table}", " */", "@Data",
                  "@EqualsAndHashCode(callSuper = true)",
                  f'@TableName("{table}")',
                  f'@CrudResource(value = "{resource}", module = "{crud_module}", search = {search_expr})',
                  f"public class {cls} extends BaseEntity {{", ""]
        for cname, jtype in cols:
            lines.append(f"    private {jtype} {cname};")
            lines.append("")
        lines.append("}")
        with open(os.path.join(entity_dir, f"{cls}.java"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines))

        mapper = f"""package {pkg}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import {pkg}.entity.{cls};
import org.apache.ibatis.annotations.Mapper;

/**
 * {table} Mapper。
 */
@Mapper
public interface {cls}Mapper extends BaseMapper<{cls}> {{
}}
"""
        with open(os.path.join(mapper_dir, f"{cls}Mapper.java"), "w", encoding="utf-8") as f:
            f.write(mapper)
        index += 1
    print(f"generated {index} entities/mappers")


if __name__ == "__main__":
    main()
