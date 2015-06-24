'use strict';

const
  Router  = require('koa-router'),
  Api     = require('../lib/api'),
  koaBody = require('koa-body');

module.exports = function(app) {
  const
    config      = app.config.api,
    api         = new Api(config.uri);

  let router = new Router({
    prefix: `/api/${config.version}`
  });

  router.use(app.jsonError);
  router.use(koaBody({multipart: true}));

  router.all('/:path*', function *() {
    let
      method  = this.method.toLowerCase(),
      body    = this.request.body,
      data    = body.fields ? body.fields : body,
      res     = yield api[method](this.params.path, data);
    this.status = res.status;
    this.body = res.response;
  });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
