'use strict';

const
  Router  = require('koa-router'),
  Api     = require('../lib/api'),
  parse   = require('co-body');

module.exports = function(app) {
  const
    config      = app.config.api,
    api         = new Api(config.uri);

  let router = new Router({
    prefix: `/api/${config.version}`
  });

  router.use(app.jsonError);

  router.all('/:path*', function *() {
    let
      method  = this.method.toLowerCase(),
      body    = yield parse.json(this),
      res     = yield api[method](this.params.path, body);
    this.status = res.status;
    this.body = res.response;
  });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
