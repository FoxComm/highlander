#!/usr/bin/env bash
set -e

# Once coursier is installed, you can use binary directly if you need it.
# If calling from other scripts, call this one to ensure binary is installed.

COURSIER="$(cd -P -- "$(dirname "$0")" && pwd -P)/coursier"
URL="https://github.com/coursier/coursier/raw/v1.0.0-RC3/coursier"

# if binary already exists,
if [ -a "${COURSIER}" ]; then
  # skip download and pass all arguments from script to binary
  "${COURSIER}" "$@"
else
  # download binary, make it executable and launch it with script arguments
  echo "No coursier binary found in ${COURSIER}, installing..."
  curl --location --output "${COURSIER}" --silent --show-error "${URL}"
  chmod +x "${COURSIER}"
  echo "Checking installation..."
  echo "----------------------------------------------------------------------"
  "${COURSIER}" --help
  echo "----------------------------------------------------------------------"
  echo "Looks good!"
  echo
  echo
  echo
  "${COURSIER}" "$@"
fi
