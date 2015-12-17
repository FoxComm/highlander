
const proxy = require('koa-proxy');

module.exports = function(app) {
  const config = app.config.elastic;

  app.use(proxy({
    host:  config.uri,
    match: /^\/api\/search\//,
    map: function(path) {
      return path.replace(/^\/api\/search\//, '/phoenix/');
    }
  }));
};
