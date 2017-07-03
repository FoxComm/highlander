#!/usr/bin/env bash
set -euo pipefail

SCALAFMT_DIR="$(cd -P -- "$(dirname "$0")" && pwd -P)"
cd "$SCALAFMT_DIR"

# shellcheck source=./shared-functions.sh
source "${SCALAFMT_DIR}"/shared-functions.sh

cd "${ROOT_DIR}"

eval "${SCALAFMT_DIR}/scalafmt.sh $(scala_files_in_last_commit)"

echo "     To update latest commit, run"
echo "           git commit --all --amend --no-edit"
echo
