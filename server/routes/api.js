const proxy = require('koa-proxy');

const matchUriRegexp = new RegExp(`^/api/v1/`);

function makeApiProxy() {
  const host = process.env.API_URL;

  return proxy({
    host,
    match: matchUriRegexp,
  });
}

exports.makeApiProxy = makeApiProxy;
