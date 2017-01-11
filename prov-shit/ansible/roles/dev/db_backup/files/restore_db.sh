#!/usr/bin/env bash

set -ue

ORDERED=/var/tmp/ordered.lst
REFRESH=/var/tmp/refresh.lst
LOCK=/var/tmp/restore_db.lock

if [ -e "$LOCK" ]; then
	echo "Restore db is in progress; Remove $LOCK if it's not true. Exit for now"
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

if [ ! -e $BACKUP ]; then
    echo "Backup file or dir $BACKUP doesn't exist"
    _exit 1
fi

db_exists $DB && { echo "Db $DB for restoring is exists, exit now"; _exit 1; }

createdb $DB

test -f $BACKUP && args="-Fc"
test -d $BACKUP && args="-Fd"

pg_restore -l $args $BACKUP | sed '/MATERIALIZED VIEW DATA/d' > $ORDERED
pg_restore -l $args $BACKUP | grep 'MATERIALIZED VIEW DATA' > $REFRESH

pg_restore -j1 -L $ORDERED $args -d $DB $BACKUP
pg_restore -j1 -L $REFRESH $args -d $DB $BACKUP

rm "$LOCK"
