#!/usr/bin/env bash

/usr/local/bin/marathon-alerts start \
    --uri http://{{consul_services.marathon}} \
    --slack-webhook {{slack_hook_url}}
