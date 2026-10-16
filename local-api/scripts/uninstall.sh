#!/system/bin/sh
# coverRoot Local API - uninstaller (run as root).
# Usage:  su -c 'sh /data/adb/local_api/uninstall.sh'
pkill -x local_api 2>/dev/null
rm -rf /data/adb/modules/coverroot_localapi
rm -rf /data/adb/local_api
echo "[uninstall] removed daemon, log and autostart module."
