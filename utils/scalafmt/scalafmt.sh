#!/usr/bin/env bash
set -e

# Not bootstrapping/installing the binary to simplify version updates.
# Once cached, works offline well.

SCALAFMT_DIR="$(dirname $0)"
SCALAFMT_VERSION="$(cat ${SCALAFMT_DIR}/scalafmt-version)"

"${SCALAFMT_DIR}/coursier.sh" launch com.geirsson:scalafmt-cli_2.11:"${SCALAFMT_VERSION}" --main org.scalafmt.cli.Cli -- --config "${SCALAFMT_DIR}/scalafmt.conf" "$@"
