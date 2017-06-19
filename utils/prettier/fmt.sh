#!/usr/bin/env bash

PROJECT_NAME=$(basename $(pwd))
PRETTIER_BIN="$(pwd)/node_modules/.bin/prettier"

if [ ! -f $PRETTIER_BIN ]; then
    echo "No prettier executable found in $PRETTIER_BIN"
    exit 1
fi

PRETTIER_OPTIONS="--single-quote --trailing-comma es5 --print-width 120"

# list of js(x) files in git diff that are not formatted in Prettier style
JS_FILES_DIFF=$(git diff HEAD --name-only --diff-filter=d | grep -E '\.(jsx?|css)$' | sed "s/$PROJECT_NAME\///g" | tr '\n' ' ')
if [ -z "$JS_FILES_DIFF" ]; then
  echo "Nothing to format in $PROJECT_NAME project"
  exit 0
fi

CMD="$PRETTIER_BIN $PRETTIER_OPTIONS -l $JS_FILES_DIFF | tr '\n' ' '"

FILES_TO_FORMAT=$(eval $CMD)
if [ -z "$FILES_TO_FORMAT" ]; then
  echo "Nothing to format in $PROJECT_NAME project"
  exit 0
fi

eval "$PRETTIER_BIN $PRETTIER_OPTIONS --write $FILES_TO_FORMAT"
