#!/bin/sh

FILE=$(readlink -f "$0")
TMPSEARCH=$(dirname "$FILE")

# search project root
while ! [ -d "${TMPSEARCH}/.git" ]
do
    TMPSEARCH=$(dirname "$TMPSEARCH")
    if [ "$TMPSEARCH" = "/" ]; then
        # .git dir not found, exit
        break
    fi
done

NPM_BIN="${TMPSEARCH}/node_modules/.bin"

if [ -d $NPM_BIN ]; then
    PATH=${NPM_BIN}:$PATH
fi

gulp hooks.run

exit $?
