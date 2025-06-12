#!/bin/bash -e

# 自动增加构建的版本的号
LAST_BUILD_NUM=$( git tag -l 'BUILD_NUM_*' | sort -rV | head -1 | sed -e 's/BUILD_NUM_//' )
[ -z "${LAST_BUILD_NUM}" ] && LAST_BUILD_NUM=0
LAST_BUILD_NUM=$( expr ${LAST_BUILD_NUM} + 1 )

export VERSION_BUILD_NUMBER="BUILD_NUM_${LAST_BUILD_NUM}"

VERSION_AND_REVISION=$( ./build.sh --full-version ${LAST_BUILD_NUM} )
export VERSION_AND_REVISION="${VERSION_BUILD_NUMBER}"
