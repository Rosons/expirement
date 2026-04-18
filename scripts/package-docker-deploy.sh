#!/usr/bin/env bash
# Packs files needed for Docker Compose deployment into a timestamped .zip
# Usage:  chmod +x scripts/package-docker-deploy.sh && ./scripts/package-docker-deploy.sh
# Requires: rsync, zip

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
FOLDER_NAME="$(basename "$REPO_ROOT")"
OUT_DIR="${REPO_ROOT}/deploy-packages"
STAGING="$(mktemp -d)"
STAGING_PROJECT="${STAGING}/${FOLDER_NAME}"
ZIP_NAME="expirement-docker-deploy-$(date +%Y%m%d-%H%M%S).zip"
ZIP_PATH="${OUT_DIR}/${ZIP_NAME}"

command -v rsync >/dev/null 2>&1 || { echo "ERROR: rsync not found"; exit 1; }
command -v zip >/dev/null 2>&1 || { echo "ERROR: zip not found"; exit 1; }

mkdir -p "${STAGING_PROJECT}"
mkdir -p "${OUT_DIR}"

rsync -a \
  --exclude='.git/' \
  --exclude='node_modules/' \
  --exclude='target/' \
  --exclude='dist/' \
  --exclude='data/' \
  --exclude='.idea/' \
  --exclude='.vscode/' \
  --exclude='.cursor/' \
  --exclude='deploy-packages/' \
  "${REPO_ROOT}/" "${STAGING_PROJECT}/"

(
  cd "${STAGING}"
  zip -r -q "${ZIP_PATH}" "${FOLDER_NAME}"
)

rm -rf "${STAGING}"

echo "OK: ${ZIP_PATH}"
