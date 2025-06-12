#! /bin/bash -e

##################################################
# BUILD.SH
#
# Build the project with default options.
#
# Return project version:          build.sh --version
# Return project full version:     build.sh --full-version <BUILD_NUMBER>
# Return project release tag:      build.sh --release-tag
# Set new version:                 build.sh -s <VERSION>
# Set new release tag:             build.sh -r <RELEASE_TAG>
# Build project:                   build.sh [--test | --publish]
#
##################################################

VERSION_FILE=./gradle.properties

function getValue() {
  if [ ! -r "$1" ]; then
    echo "ERROR: file $1 not found" >&2
    return 1
  fi
  grep "$2" "$1" | grep -v '^[[:space:]]*#' | sed -e 's/.*[[:space:]]*=[[:space:]]*\(.*\)/\1/'
  return $?
}

function getVersion() {
  getValue "${VERSION_FILE}" 'appVersion'
  return $?
}

# 动态定义构建项目时执行的Task任务
GRADLE_TASKS=""
[ "${RUN_PUBLISH}" == "YES" ] && GRADLE_TASKS="${GRADLE_TASKS} publish"
[ "${RUN_TEST}" != "YES" ] && GRADLE_TASKS="${GRADLE_TASKS} -x test"

# 执行项目构建的命令
java -version
./gradlew --version
./gradlew clean build ${GRADLE_TASKS} --refresh-dependencies -Dfile.encoding=UTF-8

exit 0
