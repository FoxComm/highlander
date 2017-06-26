#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel)"
export ROOT_DIR

# Returns modified Scala/SBT files in last commit as one line
# When calling with scalafmt, this needs an eval, e.g.
# eval "scalafmt.sh $(scala_files_in_last_commit)"
function scala_files_in_last_commit {
  git diff-tree --no-commit-id --name-only --diff-filter=d -r HEAD | grep -E '\.scala$|\.sbt$'
}
