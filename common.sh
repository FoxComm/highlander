#!/usr/bin/env bash

# Fail on unexported vars
set -ue

# Define buildable projects array
PROJECTS=(
    'ashes'
    'data-import'
    'demo/peacock'
    'developer-portal'
    'green-river'
    'hyperion'
    'intelligence/anthill'
    'intelligence/bernardo'
    'intelligence/consumers/digger-sphex'
    'intelligence/consumers/orders-anthill'
    'intelligence/consumers/orders-reviews'
    'intelligence/consumers/orders-sphex'
    'intelligence/consumers/product-activity'
    'intelligence/eggcrate'
    'intelligence/river-rock'
    'intelligence/suggester'
    'intelligence/user-simulation'
    'isaac'
    'messaging'
    'middlewarehouse'
    'middlewarehouse/common/db/seeds'
    'middlewarehouse/consumers/capture'
    'middlewarehouse/consumers/customer-groups'
    'middlewarehouse/consumers/gift-cards'
    'middlewarehouse/consumers/shipments'
    'middlewarehouse/consumers/shipstation'
    'middlewarehouse/consumers/stock-items'
    'middlewarehouse/elasticmanager'
    'onboarding'
    'onboarding/ui'
    'phoenix-scala'
    'phoenix-scala/seeder'
    'solomon'
    'tabernacle/docker/neo4j'
    'tabernacle/docker/neo4j_reset'
)

# Save Highlander directory
HIGHLANDER_PATH=$PWD

# Define helper functions
function write() {
    if $DEBUG; then
        echo -e "[BUILDER]" $1
    fi
}

function contains() {
    local n=$#
    local value=${!n}
    for ((i=1;i < $#;i++)) {
        if [ "${!i}" == "${value}" ]; then
            echo "y"
            return 0
        fi
    }
    echo "n"
    return 1
}
