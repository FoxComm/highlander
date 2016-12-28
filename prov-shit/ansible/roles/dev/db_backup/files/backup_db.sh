#!/usr/bin/env bash

set -ue

LOCK=/var/tmp/backup_db.lock

if [ -e "$LOCK" ]; then
	echo "Backup db is in progress; Remove $LOCK if it's not true. Exit for now"
	exit 1
fi

touch "$LOCK"

usage() {
	echo "Usage: $0 <backup> <db_name>"
}

_exit() {
	rm "$LOCK"
	exit $1
}

db_exists() {
	psql $1 -c '\q' > /dev/null 2>&1
}

if [ $# -ne 2 ]; then
	usage
	_exit 1
fi

BACKUP=$1
DB=$2

if [ -e $BACKUP ]; then
 echo "Backup file or dir $BACKUP already exist"
 _exit 1
fi

db_exists $DB || { echo "Db $DB not exists, exit now"; _exit 1; }

pg_dump -Fc -Z6 -d $DB -f $BACKUP

rm "$LOCK"
