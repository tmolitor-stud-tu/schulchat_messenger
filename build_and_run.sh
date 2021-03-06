#!/bin/bash

if [[ "$1" == "" ]]; then
	echo "Usage: $(basename "$0") appID"
	exit 1
fi

app="$(basename "$1")"
cd "$(dirname "$0")"
shift;

if [[ ! -e local/$app.settings ]]; then
	echo "App settings for '$app' not found at 'local/$app.settings'!"
	exit 2
fi

. local/$app.settings

build="$gradleBuild"
if [[ "$@" != "" ]]; then
	build=""
fi

export ORG_GRADLE_PROJECT_gcm_defaultSenderId="$(python3 load_value.py $app "project_number")"
export ORG_GRADLE_PROJECT_google_app_id="$(python3 load_value.py $app "mobilesdk_app_id")"

echo "app_secret = '$ORG_GRADLE_PROJECT_mAppSecret'"
echo "signing with key '$ORG_GRADLE_PROJECT_mKeyAlias'"
echo "gcm_defaultSenderId = '$ORG_GRADLE_PROJECT_gcm_defaultSenderId'"
echo "google_app_id = '$ORG_GRADLE_PROJECT_google_app_id'"

./gradlew $build $@ || exit 1
real_apk="$(realpath $apk)"
dest="$(basename "$real_apk" | sed 's/-unsigned/-signed/g')"
jarsigner -keystore "$ORG_GRADLE_PROJECT_mStoreFile" -storepass "$ORG_GRADLE_PROJECT_mStorePassword" -keypass "$ORG_GRADLE_PROJECT_mKeyPassword" "$real_apk" "$ORG_GRADLE_PROJECT_mKeyAlias" && $ANDROID_HOME/build-tools/28.0.3/zipalign -f 4 "$real_apk" "$dest" && adb install -r "$dest" && (
	sleep 1
	trap "adb shell am force-stop $app & exit" INT
	adb shell am start -n $app/$activity &
	PID=""
	while [ -z "$PID" ]; do
		PID="$(adb shell ps | grep $app | tr -s [:space:] ' ' | cut -d' ' -f2)"
		sleep 0.1
	done
	echo "Found PID: $PID"
	adb logcat -T 128 -v threadtime | egrep "($PID)"
)
