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

if [[ $2 == "start" ]]; then
	trap "adb shell am force-stop $app & exit" INT
	adb shell am start -n $app/$activity &
fi
PID=""
while [ -z "$PID" ]; do
	PID="$(adb shell ps | grep $app | tr -s [:space:] ' ' | cut -d' ' -f2)"
	sleep 0.1
done
echo "Found PID: $PID"
adb logcat -T 128 -v threadtime | egrep "($PID)"

