#!/bin/bash
# SumCompare GUI Launcher Script

echo "Starting SumCompare GUI..."
cd "$(dirname "$0")"
mvn javafx:run
