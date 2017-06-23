#!/bin/bash
set -euo pipefail

mix run priv/seeds/clothes_accessories_categories.exs
mix run priv/seeds/clothes_schema.exs