const proxy = require('koa-proxy');

const matchUriRegexp = new RegExp(`^/api/search/`);

function makeElasticProxy() {
  const host = process.env.API_URL;

  return proxy({
    host,
    match: matchUriRegexp,
  });
}

exports.makeElasticProxy = makeElasticProxy;
