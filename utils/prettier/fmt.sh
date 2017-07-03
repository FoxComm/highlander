#!/usr/bin/env bash

PROJECT_NAME=$(basename $(pwd))
PRETTIER_BIN="$(pwd)/node_modules/.bin/prettier"

if [ ! -f $PRETTIER_BIN ]; then
    echo "No prettier executable found in $PRETTIER_BIN"
    exit 1
fi

PRETTIER_OPTIONS="--single-quote --trailing-comma es5 --print-width 120"

if [ $# -eq 0 ]; then
    # list of js(x) files in git diff that are not formatted in Prettier style
    JSCSS_FILES_DIFF=$(git diff HEAD --name-only --diff-filter=d | grep -E '\.(jsx?|css)$' | sed "s/$PROJECT_NAME\///g" | tr '\n' ' ')
    if [ -z "$JSCSS_FILES_DIFF" ]; then
        echo "Nothing to format in $PROJECT_NAME project"
        exit 0
    fi
else
  if [ ! -d $1 ]; then
    echo "No \"$1\" directory found"
    exit 0
  fi
  JSCSS_FILES_DIFF=$(find $1 -type f \( -iname \*.js -o -iname \*.jsx -o -iname \*.css \) | tr '\n' ' ')
fi

CMD="$PRETTIER_BIN $PRETTIER_OPTIONS -l $JSCSS_FILES_DIFF | tr '\n' ' '"

FILES_TO_FORMAT=$(eval $CMD)
if [ -z "$FILES_TO_FORMAT" ]; then
  echo "Nothing to format"
  exit 0
fi

eval "$PRETTIER_BIN $PRETTIER_OPTIONS --write $FILES_TO_FORMAT"
