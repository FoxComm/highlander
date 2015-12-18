ashes: sh -c 'npm run dev'
nginx: sh -c 'if hash openresty 2>/dev/null; then openresty -c `pwd`/config/nginx.development.conf; else nginx -c `pwd`/config/nginx.development.conf; fi'
