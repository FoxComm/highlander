
const proxy = require('koa-proxy');

module.exports = function(app) {
  const config = app.config.elastic;

  const proxyHandler = proxy({
    host:  config.uri,
    match: /^\/api\/search\//,
    map: function(path) {
      return path.replace(/^\/api\/search\//, '/phoenix/');
    }
  });

  function* formatHandler() {
    const body = JSON.parse(this.body);

    body.hits.result = body.hits.hits.map(hit => hit._source);
    body.hits.hits = void 0;
    body.hits.pagination = {total: body.hits.total};
    body.hits.total = void 0;

    this.body = JSON.stringify(body.hits);
  }

  function* proxyAndFormat(next) {
    yield proxyHandler.call(this, next);
    yield formatHandler.call(this);
  }

  app.use(proxyAndFormat);
};
