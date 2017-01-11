
const proxy = require('koa-proxy');

module.exports = function(app) {
  const config = app.config.api;
  const matchUriRegexp = new RegExp(`^/zipkin/`);

  app.use(proxy({
    host:  config.host,
    match: matchUriRegexp,
  }));

};
