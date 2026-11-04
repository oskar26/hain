#!/usr/bin/env bash
# T-190: release smoke test + start time on the CI emulator (screens.yml, job "release-smoke").
#
#   tools/ci/release_smoke.sh <release.apk> [<before.apk>] [<mapping.txt>]
#
# 1. Installs the minified release APK (app.hain) and starts it: the process must still live after a
#    few seconds and the crash log must stay empty (catches missing R8 keep rules at start).
# 2. Checks that ProfileInstaller put the baseline profile in place (dump of the ART profile).
# 3. Measures cold start ("am start -W", TotalTime, median of $RUNS) of
#      before = <before.apk> (the nightly debug APK of main, app.hain.debug) and
#      after  = the release APK, each as installed and after "compile -m speed-profile".
#    The before APK also proves the debug package app.hain.debug still installs and starts.
# Writes smoke/report.md (also shown in the job summary) and smoke/logcat.txt.
set -uo pipefail

REL="$1"
BEFORE="${2:-}"
MAPPING="${3:-}"
RUNS="${RUNS:-7}"
ACTIVITY="app.hain.MainActivity"
OUT=smoke
mkdir -p "$OUT"
REPORT="$OUT/report.md"
FAIL=0

log() { echo "$*" | tee -a "$REPORT"; }

adb wait-for-device
adb root > /dev/null 2>&1 || true
sleep 3
adb wait-for-device
adb logcat -c || true

{
  echo "## Release smoke test (T-190)"
  echo
  echo "Emulator: API $(adb shell getprop ro.build.version.sdk | tr -d '\r'), ABIs $(adb shell getprop ro.product.cpu.abilist | tr -d '\r')"
  echo
} > "$REPORT"

cold_start() { # pkg -> prints TotalTime in ms (empty on failure)
  adb shell am force-stop "$1"
  sleep 1
  adb shell am start -W -n "$1/$ACTIVITY" 2>/dev/null | tr -d '\r' | awk '/TotalTime/ {print $2}'
}

median() { sort -n | awk '{a[NR]=$1} END {if (NR==0) {print "-"} else if (NR%2) {print a[(NR+1)/2]} else {print int((a[NR/2]+a[NR/2+1])/2)}}'; }

measure() { # pkg label
  local pkg="$1" label="$2" t all=""
  for _ in $(seq 1 "$RUNS"); do
    t=$(cold_start "$pkg")
    [ -n "$t" ] && all="$all $t"
    sleep 2
  done
  local med
  med=$(echo "$all" | tr ' ' '\n' | grep -v '^$' | median)
  log "| $label | $med ms | ${all# } |"
}

alive_check() { # pkg label
  local pkg="$1" label="$2"
  adb shell am force-stop "$pkg"
  adb logcat -c -b crash > /dev/null 2>&1 || true
  adb shell am start -W -n "$pkg/$ACTIVITY" > /dev/null 2>&1
  sleep 8
  local pid crash
  pid=$(adb shell pidof "$pkg" | tr -d '\r')
  crash=$(adb logcat -d -b crash 2>/dev/null | grep -c "$pkg" || true)
  if [ -n "$pid" ] && [ "$crash" = "0" ]; then
    log "- ✅ $label ($pkg) starts and is still running after 8 s (pid $pid)"
  else
    log "- ❌ $label ($pkg) did not survive the start (pid '${pid}', crash lines $crash)"
    adb logcat -d -b crash 2>/dev/null | tail -60 | tee -a "$OUT/crash.txt"
    FAIL=1
  fi
}

compile_profile() { # pkg
  adb shell cmd package compile -f -m speed-profile "$1" > /dev/null 2>&1
}

# --- release APK ---------------------------------------------------------------------------------
if ! adb install -r "$REL" 2>&1 | tee -a "$OUT/install.txt" | grep -q Success; then
  log "- ❌ release APK did not install: $(tail -1 "$OUT/install.txt")"
  exit 1
fi
alive_check app.hain "Release APK (R8 + shrinkResources)"

# ProfileInstaller writes the baseline profile a few seconds after the first start.
sleep 6
adb shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE \
  -n app.hain/androidx.profileinstaller.ProfileInstallReceiver > /dev/null 2>&1 || true
sleep 3
adb shell cmd package dump-profiles app.hain > /dev/null 2>&1 || true
adb shell cat /data/misc/profman/app.hain-primary.prof.txt 2>/dev/null | tr -d '\r' > "$OUT/art-profile.txt"
# R8 renamed most classes: map them back with mapping.txt to count methods of Hain's own code
COUNTS=$(python3 - "$OUT/art-profile.txt" "$MAPPING" <<'PY'
import re, sys
names = {}
if len(sys.argv) > 2 and sys.argv[2]:
    try:
        for line in open(sys.argv[2]):
            if not line.startswith((" ", "#")) and " -> " in line and line.rstrip().endswith(":"):
                orig, ob = line.rstrip()[:-1].split(" -> ")
                names[ob.strip()] = orig.strip()
    except OSError:
        pass
total = own = 0
for line in open(sys.argv[1]):
    m = re.match(r"^\s*\S+ ([\w$.]+)\.[\w$<>]+\(", line)
    if m:
        total += 1
        own += names.get(m.group(1), m.group(1)).startswith("app.hain.")
print(total, own)
PY
)
read -r ALL_METHODS APP_METHODS <<< "${COUNTS:-0 0}"
log "- Baseline profile on the phone: $ALL_METHODS methods, $APP_METHODS of them in Hain's own code"
if [ "${ALL_METHODS:-0}" = "0" ]; then
  log "- ❌ no baseline profile was installed"
  FAIL=1
fi

if [ -n "$BEFORE" ] && [ -f "$BEFORE" ]; then
  if adb install -r "$BEFORE" 2>&1 | tee -a "$OUT/install.txt" | grep -q Success; then
    alive_check app.hain.debug "Debug APK of main (nightly)"
  else
    log "- ⚠️ before APK did not install: $(tail -1 "$OUT/install.txt")"
    BEFORE=""
  fi
fi

log ""
log "### Cold start (am start -W, TotalTime, median of $RUNS, no brain loaded)"
log ""
log "| Build | Median | Runs |"
log "|---|---|---|"
if [ -n "$BEFORE" ]; then
  measure app.hain.debug "before · nightly debug APK of main, as installed"
fi
measure app.hain "after · release APK (R8 + profile), as installed"
if [ -n "$BEFORE" ]; then
  compile_profile app.hain.debug
  measure app.hain.debug "before · nightly debug APK, after compile -m speed-profile"
fi
compile_profile app.hain
measure app.hain "after · release APK, after compile -m speed-profile"
log ""
log "The emulator runs on a shared CI machine; read the numbers as a trend, not as phone times."

adb shell dumpsys package dexopt 2>/dev/null | tr -d '\r' | grep -A3 '\[app.hain' > "$OUT/dexopt.txt" || true
adb logcat -d > "$OUT/logcat.txt" 2>/dev/null || true

# a last check that the release build is still alive after all the starts
alive_check app.hain "Release APK after the measurements"
exit $FAIL
