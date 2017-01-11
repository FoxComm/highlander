#!/usr/bin/env bash

set -ue

LOCK=/var/tmp/backup_zk.lock

if [ -e "$LOCK" ]; then
    echo "Zookeeper backup is in progress; Remove $LOCK if it's not true. Exit for now"
    exit 1
fi

touch "$LOCK"

usage() {
    echo "Usage: $0 <backup> <zk_data_dir>"
}

_exit() {
    rm "$LOCK"
    exit $1
}

if [ $# -ne 2 ]; then
    usage
    _exit 1
fi

BACKUP=$1
ZK_DIR=$2

if [ -e $BACKUP ]; then
    echo "Backup file or dir $BACKUP already exist"
    _exit 1
fi

test -d $ZK_DIR
tar -czvf $BACKUP $ZK_DIR

rm "$LOCK"
