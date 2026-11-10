#!/bin/sh
set -eu
mkdir -p /tmp/atlasshard
chown -R atlasshard:atlasshard /tmp/atlasshard
exec su-exec atlasshard:atlasshard java -jar /app/app.jar
