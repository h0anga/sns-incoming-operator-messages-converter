#!/usr/bin/env bash

set -e

function usage() {
  echo >&2 "Usage: $0 [-a] (build fat jar) [-t target (darwin|docker)]"
}

#The colon represents an argument!
while getopts :ft:i opt
do
  case "$opt" in
    f)
        echo >&2 "Building Fat Jar..."
        sbt assembly
        echo >&2 "Built Fat Jar Successfully - look for target/scala-2.12/xmlJsonConverter.jar"
        ;;
    t)
        TARGET="$OPTARG"
        if [[ "${TARGET}" == "darwin" ]]
        then
          echo >&2 "Building native Image for Mac..."
          native-image -jar ./target/scala-2.12/xmlJsonConverter.jar macXmlToJsonConverter
          echo >&2 "Built native-image for: " $TARGET
        elif [[ "${TARGET}" == "docker" ]]
        then
          echo >&2 "Building native Image for linux on Docker..."
          docker build -f ./NativeImageDockerfile -t sns/graal-native-image-builder .
          docker run --rm -v $PWD:/project sns/graal-native-image-builder:latest native-image -jar /project/target/scala-2.12/xmlJsonConverter.jar /project/linuxXmlToJsonConverter
          echo >&2 "Built native-image for: " $TARGET
        fi
        ;;
    i)
        echo >&2 "Building Docker Image to run native executable..."
        docker build -f ./Dockerfile -t sns/xml-json-converter .
        echo >&2 "Built image sns/xml-json-converter"
        ;;
    *)
        usage
        exit 1
        ;;
  esac

done

