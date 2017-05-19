#!/usr/bin/env bash

/usr/local/bin/marathon-alerts start \
    --uri http://{{marathon_server}} \
    --slack-webhook {{slack_hook_url}}
