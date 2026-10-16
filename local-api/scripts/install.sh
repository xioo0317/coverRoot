#!/system/bin/sh
# coverRoot Local API - one-shot installer (Android, run as root).
# Usage:  su -c 'sh /path/to/install.sh'
# Keep local_api / install.sh / uninstall.sh in the same folder.
#
# VERSION must match local-api/include/version.hpp SERVER_VERSION.
VERSION="1.17.0"

DIR=/data/adb/local_api
MODDIR=/data/adb/modules/coverroot_localapi
SELF_DIR=$(dirname "$0")

say() { echo "[install] $*"; }

# ---- 0. root check ------------------------------------------------------
if [ "$(id -u)" != "0" ]; then
    say "ERROR: not root. Run: su -c 'sh $0'"
    exit 1
fi

# ---- 1. environment diagnostics ----------------------------------------
# Collected on every install so troubleshooting logs always have them.
say "== environment =="
say "uid: $(id -u 2>/dev/null)  selinux: $(getenforce 2>/dev/null || echo unknown)"
POLICY_FOUND=0
for p in /data/adb/magisk/magiskpolicy /data/adb/ksu/bin/magiskpolicy /data/adb/ap/bin/magiskpolicy; do
    if [ -x "$p" ]; then
        say "policy tool found: $p"
        POLICY_FOUND=1
    else
        say "policy tool missing: $p"
    fi
done
if command -v magiskpolicy >/dev/null 2>&1; then
    say "policy tool on PATH: $(command -v magiskpolicy)"
    POLICY_FOUND=1
fi
if [ "$POLICY_FOUND" != "1" ] && [ "$(getenforce 2>/dev/null)" = "Enforcing" ]; then
    say "NOTE: no magiskpolicy found while SELinux is Enforcing."
    say "      If the manager later reports 'Permission denied', your root"
    say "      solution cannot grant the appdomain connectto rule. Try"
    say "      updating your root solution, or test with 'setenforce 0'."
fi

# ---- 2. install daemon binary -------------------------------------------
if [ ! -f "$SELF_DIR/local_api" ]; then
    say "ERROR: local_api not found next to this script ($SELF_DIR/local_api)"
    exit 1
fi
mkdir -p "$DIR"
cp "$SELF_DIR/local_api" "$DIR/local_api"
cp "$SELF_DIR/uninstall.sh" "$DIR/uninstall.sh" 2>/dev/null
chmod 755 "$DIR/local_api" "$DIR/uninstall.sh" 2>/dev/null
say "installed: $DIR/local_api (v$VERSION)"

# ---- 3. restart daemon ---------------------------------------------------
# v1.16.4+ refuses to start a second copy over a live listener, so stop first.
pkill -x local_api 2>/dev/null && say "stopped old daemon"
nohup "$DIR/local_api" router @local_api >>"$DIR/daemon.log" 2>&1 &
sleep 1

# ---- 4. verify -----------------------------------------------------------
if grep -q "@local_api" /proc/net/unix 2>/dev/null; then
    say "daemon is listening on @local_api  [OK]"
else
    say "WARN: @local_api not visible in /proc/net/unix yet."
    say "      Check the log: cat $DIR/daemon.log"
fi

# ---- 5. boot autostart module (Magisk / KernelSU / APatch compatible) ----
mkdir -p "$MODDIR"
cat > "$MODDIR/module.prop" <<'EOF'
id=coverroot_localapi
name=coverRoot Local API
version=1.17.0
versionCode=170
author=coverRoot
description=Starts the coverRoot local_api daemon on boot.
EOF
cat > "$MODDIR/service.sh" <<'EOF'
#!/system/bin/sh
# coverRoot local_api autostart (late_start service mode)
(
    while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done
    exec /data/adb/local_api/local_api router @local_api >>/data/adb/local_api/daemon.log 2>&1
) &
EOF
chmod 755 "$MODDIR/service.sh"
say "autostart module written: $MODDIR (active after next reboot)"
say "done. Now install the manager APK and open the app."
