#!/bin/bash

set -e  # exit immediately if any command fails

# ---- Config (adjust if needed) ----
APP_DIR="/opt/vegfresh/backend/VEG-BACKEND"
SERVICE_NAME="vegfresh.service"
BRANCH="main"
RUN_USER="veggofresh"

echo "===================================================="
echo " Starting deployment: $(date '+%Y-%m-%d %H:%M:%S')"
echo "===================================================="

cd "$APP_DIR"

echo "[1/5] Stopping $SERVICE_NAME to free up RAM and file locks..."
# Stop the service first so it releases the old .jar file
systemctl stop "$SERVICE_NAME" || true

echo "[2/5] Fetching latest code from origin/$BRANCH..."
git fetch origin
git reset --hard "origin/$BRANCH"

# DESTROY GHOST FILES: This deletes all untracked files in the src folder (like V137)
git clean -fd

echo "[3/5] Cleaning old build files forcefully..."
# Use the OS 'rm' command instead of Maven's clean plugin to guarantee deletion
rm -rf "$APP_DIR/target"

echo "[4/5] Building with Maven (skipping tests)..."
mvn clean package -DskipTests

echo "[5/5] Fixing ownership and starting service..."
chown -R "$RUN_USER":"$RUN_USER" "$APP_DIR"
systemctl start "$SERVICE_NAME"

# Give the app a few seconds to boot before checking status
sleep 5

echo "===================================================="
echo " Deployment finished. Service status:"
echo "===================================================="
systemctl status "$SERVICE_NAME" --no-pager

echo ""
echo "Done: $(date '+%Y-%m-%d %H:%M:%S')"
