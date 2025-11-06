#!/bin/bash
# SumCompare GUI Launcher Script

echo "Starting SumCompare GUI..."
cd "$(dirname "$0")"
export MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=5005"
mvn javafx:run
