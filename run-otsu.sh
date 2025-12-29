#!/bin/bash
# Script to run TestOtsu

if [ -z "$1" ]; then
    echo "Usage: ./run-otsu.sh <image-file-path>"
    echo "Example: ./run-otsu.sh images/test.jpg"
    exit 1
fi

CLASSPATH="bin:lib/slf4j-api-1.7.12.jar:lib/slf4j-log4j12-1.7.12.jar:lib/log4j-1.2.17.jar:lib/jai_imageio.jar"

java -cp "$CLASSPATH" com.icafe4j.test.TestOtsu "$1"