#!/usr/bin/env sh

echo "dependencies.sh started"

set -eu
apk add --no-cache py-pip bash

# Install docker-compose
pip install --no-cache-dir docker-compose
docker-compose -v

echo "dependencies.sh stopped"