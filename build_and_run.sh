#!/bin/bash

. local/local_settings.sh

./gradlew assemblePlaystoreRelease $@ || exit 1
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
