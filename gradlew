#!/bin/sh
#
# Gradle start up script for POSIX generated for BITTO-TV 2026.
#
set -e
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
