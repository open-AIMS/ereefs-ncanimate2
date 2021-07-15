#!/usr/bin/env bash

#
# Build the Docker image based on Dockerfile. This script assumes the user has previously
# compiled this project and the "ereefs-ncanimate2-frame" project.
#

# Identify the directory of this script, and the root directory of the project.
SCRIPT="$(readlink -f "$0")"
SCRIPT_PATH="$(dirname "$SCRIPT")"
PROJECT_ROOT="$(readlink --canonicalize "$SCRIPT_PATH/..")"

# Find the JAR files
ABSOLUTE_PATH_NCANIMATE_JAR_FILE=$(ls "$PROJECT_ROOT/target/ereefs-ncanimate2-"*"-jar-with-dependencies.jar")
NCANIMATE_JAR_NAME=$(basename "$ABSOLUTE_PATH_NCANIMATE_JAR_FILE")

ABSOLUTE_PATH_NCANIMATE_FRAME_JAR_FILE=$(ls "$PROJECT_ROOT/../ereefs-ncanimate2-frame/target/ereefs-ncanimate2-frame-"*"-jar-with-dependencies.jar")
NCANIMATE_FRAME_JAR_NAME=$(basename "$ABSOLUTE_PATH_NCANIMATE_FRAME_JAR_FILE")

if [ ! -f "$ABSOLUTE_PATH_NCANIMATE_JAR_FILE" ]; then
    echo "ERROR: NcAnimate jar file not found: ${ABSOLUTE_PATH_NCANIMATE_JAR_FILE}. Run mvn package in ereefs-ncanimate2 project"
    exit 1
fi

if [ ! -f "$ABSOLUTE_PATH_NCANIMATE_FRAME_JAR_FILE" ]; then
    echo "ERROR: NcAnimate frame jar file not found: ${ABSOLUTE_PATH_NCANIMATE_FRAME_JAR_FILE}. Run mvn package in ereefs-ncanimate2-frame project"
    exit 1
fi

cp "${ABSOLUTE_PATH_NCANIMATE_FRAME_JAR_FILE}" "${PROJECT_ROOT}/target/"

# Build the Docker image.
docker build \
    -t "ereefs-ncanimate-test:latest" \
    -f "$PROJECT_ROOT/Dockerfile" \
    --build-arg NCANIMATE_JAR_NAME="$NCANIMATE_JAR_NAME" \
    --build-arg NCANIMATE_FRAME_JAR_NAME="$NCANIMATE_FRAME_JAR_NAME" \
    --force-rm \
    "$PROJECT_ROOT"

rm "${PROJECT_ROOT}/target/${NCANIMATE_FRAME_JAR_NAME}"
