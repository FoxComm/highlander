# Make backends stable again!

Persistent caching server for you backend. Only caches successful answers.
Also allows you to override responses from origin.

Usage:

    npm install -g ./highlander/dev-caching-proxy
    dev-caching-proxy -c <cache-dir> -t https://stage-tpg.foxcommerce.com

Or without installing:

    ./dev-caching-proxy/src/server.js --help

For overriding just put your answer under `<cache-dir>/<url>.json` path.