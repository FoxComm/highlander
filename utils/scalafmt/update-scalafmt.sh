#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -P -- "$(dirname "$0")" && git rev-parse --show-toplevel)"
SCALAFMT="$(cd -P -- "$(dirname "$0")" && pwd -P)/scalafmt.sh"

echo "Hey there! We're moving to a new approach with scalafmt!"
echo "From now on, it won't ship as SBT plugin, but as a standalone executable."
echo "You can find it in ${SCALAFMT}"
echo "To support different workflows, you are now responsible for formatting sources."
echo "Post-commit git hook will check if all changed Scala files are formatted."
echo "Don't worry, you can still commit :)"
echo "Post-commit hook provides instructions how to format and amend your commit with proper formatting."
echo
echo "Editor integrations are described in http://scalameta.org/scalafmt/#IntelliJ and http://scalameta.org/scalafmt/#Vim"
echo
echo "This script installs scalafmt with dependencies, configures git hooks and triggers initial formatting."
echo "Running scalafmt before you pull master is important to ensure you have no formatting-only merge conflicts between scalafmt updates and configuration changes."
echo
read -rp "Press enter to continue!"
echo
echo

REQUIRED_GIT_VERSION=2.9.0
CURRENT_GIT_VERSION=$(git --version| cut -d' ' -f3)

# This function allows to compare git app versions using pure alphabetical order
# $> echo "$(version "2.9.0")"
# $> 002009000
function version { echo "$@" | gawk -F. '{ printf("%03d%03d%03d\n", $1,$2,$3); }'; }

if [ "$(version "$REQUIRED_GIT_VERSION")" -gt "$(version "$CURRENT_GIT_VERSION")" ]; then
  echo "Your current git version is $CURRENT_GIT_VERSION, but it should be at least $REQUIRED_GIT_VERSION"
  echo "Please update your git"
  exit 1
fi

echo "Setting utils/git-hooks dir as a default hooks dir..."
git config core.hooksPath "${ROOT_DIR}/utils/git-hooks"
echo

echo "Running scalafmt on all sources... (only needs to be done once!)"
eval "${SCALAFMT} ${ROOT_DIR}"
echo

echo "Checking that sources are formatted correctly..."
eval "${SCALAFMT} ${ROOT_DIR} --test"
echo

echo
echo "All done! Happy hacking!"
