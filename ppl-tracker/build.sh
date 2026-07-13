#!/usr/bin/env bash
###############################################################################
# PPL Tracker – manual command-line APK build.
#
# This environment cannot reach Google's Maven repo (dl.google.com is blocked
# by egress policy), so the Android Gradle Plugin / AndroidX / Compose cannot
# be downloaded and a normal `./gradlew assembleDebug` is not possible here.
#
# This script builds the debug APK directly from the framework toolchain:
#   kotlinc -> dx (dex) -> aapt2 (resources) -> zipalign -> apksigner
#
# Tools it expects (installed in this session):
#   - android.jar  (API 34)   : $SDK_JAR
#   - kotlinc jars            : /opt/kotlinc-lib
#   - aapt2, zipalign, apksigner (apt: android-sdk-build-tools / apksigner)
#   - dalvik-exchange (apt: dalvik-exchange)  -- the dx dexer
#
# On a machine with a normal Android SDK, just use the Gradle project instead:
#   ./gradlew assembleDebug
###############################################################################
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
APP="$HERE/app/src/main"
OUT="$HERE/build"
PKG="com.ppl.tracker"

SDK_JAR="${SDK_JAR:-/opt/android-mini/platforms/android-34/android.jar}"
KLIB="/opt/kotlinc-lib"
KCP="$(cat "$KLIB/kcp.txt")"
STDLIB="$KLIB/kotlin-stdlib.jar"

AAPT2="${AAPT2:-aapt2}"
ZIPALIGN="${ZIPALIGN:-zipalign}"
APKSIGNER="${APKSIGNER:-apksigner}"
DX="${DX:-dalvik-exchange}"

echo "==> clean"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/merged" "$OUT/res"

echo "==> [1/7] compile Kotlin"
KT_SRC=$(find "$APP/kotlin" -name '*.kt')
java -cp "$KCP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -no-reflect \
    -classpath "$SDK_JAR:$STDLIB" \
    -jvm-target 1.8 \
    -d "$OUT/classes" \
    $KT_SRC

echo "==> [2/7] merge app classes + kotlin stdlib (strip multi-release/module-info)"
( cd "$OUT/merged" && unzip -oq "$STDLIB" )
rm -rf "$OUT/merged/META-INF" "$OUT/merged/module-info.class"
cp -r "$OUT/classes/." "$OUT/merged/"

echo "==> [3/7] dex -> classes.dex"
"$DX" --dex --min-sdk-version=26 --output="$OUT/classes.dex" "$OUT/merged"

echo "==> [4/7] compile resources (aapt2)"
"$AAPT2" compile --dir "$APP/res" -o "$OUT/res.zip"

echo "==> [5/7] link resources + manifest (aapt2)"
# aapt2 needs a package on the manifest; inject it into a build-time copy so the
# committed manifest stays clean for a normal Gradle build.
sed 's#<manifest xmlns:android="http://schemas.android.com/apk/res/android">#<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="'"$PKG"'">#' \
    "$APP/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"

"$AAPT2" link \
    -o "$OUT/app-unaligned.apk" \
    -I "$SDK_JAR" \
    --manifest "$OUT/AndroidManifest.xml" \
    --min-sdk-version 26 \
    --target-sdk-version 34 \
    --version-code 1 \
    --version-name "1.0" \
    "$OUT/res.zip"

echo "==> [6/7] add classes.dex + align"
( cd "$OUT" && cp app-unaligned.apk app-withdex.apk && zip -jq app-withdex.apk classes.dex )
"$ZIPALIGN" -f -p 4 "$OUT/app-withdex.apk" "$OUT/app-aligned.apk"

echo "==> [7/7] sign (debug keystore)"
KS="$OUT/debug.keystore"
if [ ! -f "$HERE/debug.keystore" ]; then
    keytool -genkeypair -v -keystore "$KS" -storepass android -keypass android \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
else
    cp "$HERE/debug.keystore" "$KS"
fi
"$APKSIGNER" sign \
    --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --min-sdk-version 26 \
    --out "$OUT/app-debug.apk" \
    "$OUT/app-aligned.apk"

"$APKSIGNER" verify --min-sdk-version 26 "$OUT/app-debug.apk" && echo "signature OK"

echo
echo "BUILD OK -> $OUT/app-debug.apk"
ls -la "$OUT/app-debug.apk"
