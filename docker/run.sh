#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "[1/3] Building mod..."
cd "$PROJECT_DIR"
./gradlew build --no-daemon

echo "[2/3] Copying JAR to mods folder..."
cp build/libs/proxy_protocol_support-*.jar "$SCRIPT_DIR/forge/mods/"

echo "[3/3] Starting Docker containers..."
cd "$SCRIPT_DIR"
docker compose up -d --build
docker compose logs -f
