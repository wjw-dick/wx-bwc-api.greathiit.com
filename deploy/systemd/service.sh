#!/usr/bin/env bash
# ============================================================================
# 校园保卫 SaaS API 裸机部署脚本（systemd）
# 用法：sudo bash deploy/systemd/service.sh {install|start|stop|restart|status|logs|uninstall}
# 说明：脚本只做服务管理，不执行 Maven 打包；请先准备好 bwc-api.jar。
# ============================================================================
set -euo pipefail

APP_NAME="bwc-api"
APP_DIR="${BWC_APP_DIR:-/opt/bwc}"
JAR="${BWC_JAR:-${APP_DIR}/bwc-api.jar}"
ENV_DIR="${BWC_ENV_DIR:-/etc/bwc}"
ENV_FILE="${ENV_DIR}/bwc.env"
SERVICE_FILE="/etc/systemd/system/${APP_NAME}.service"
RUN_USER="${BWC_RUN_USER:-bwc}"

usage() {
  echo "用法: $0 {install|start|stop|restart|status|logs|uninstall}"
  exit 1
}

require_root() {
  if [[ "${EUID}" -ne 0 ]]; then
    echo "请使用 root 或 sudo 执行" >&2
    exit 1
  fi
}

install_service() {
  require_root
  id -u "${RUN_USER}" >/dev/null 2>&1 || useradd --system --home "${APP_DIR}" --shell /sbin/nologin "${RUN_USER}"
  mkdir -p "${APP_DIR}"/{uploads,logs} "${ENV_DIR}"
  chown -R "${RUN_USER}:${RUN_USER}" "${APP_DIR}"
  if [[ ! -f "${JAR}" ]]; then
    echo "未找到 ${JAR}，请先上传 jar 包" >&2
    exit 1
  fi
  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "未找到 ${ENV_FILE}，请参照 deploy/.env.example 配置后重试" >&2
    exit 1
  fi
  install -m 0644 "$(dirname "$0")/bwc-api.service" "${SERVICE_FILE}"
  systemctl daemon-reload
  systemctl enable "${APP_NAME}"
  systemctl restart "${APP_NAME}"
  echo "已安装并启动 ${APP_NAME}"
}

case "${1:-}" in
  install) install_service ;;
  start) require_root; systemctl start "${APP_NAME}" ;;
  stop) require_root; systemctl stop "${APP_NAME}" ;;
  restart) require_root; systemctl restart "${APP_NAME}" ;;
  status) systemctl status "${APP_NAME}" --no-pager ;;
  logs) journalctl -u "${APP_NAME}" -f -n 200 ;;
  uninstall)
    require_root
    systemctl disable --now "${APP_NAME}" || true
    rm -f "${SERVICE_FILE}"
    systemctl daemon-reload
    echo "已卸载 ${APP_NAME}（数据与上传目录保留）"
    ;;
  *) usage ;;
esac
