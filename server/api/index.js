'use strict';

const
  Router  = require('koa-router'),
  Api     = require('../lib/api'),
  koaBody = require('koa-body');

module.exports = function(app) {
  const
    config      = app.config.api,
    api         = new Api(config.uri, config.auth);

  let router = new Router({
    prefix: `/api/${config.version}`
  });

  router.use(app.jsonError);
  router.use(koaBody({multipart: true}));

  router.all('/:path*', function *() {
    let
      query   = this.request.query,
      body    = this.request.body,
      method  = this.method.toLowerCase();

    function getData() {
      switch(method) {
        case 'get': return query;
        default: return body.fields ? body.fields : body;
      }
    }

    let res = yield api[method](this.params.path, getData());
    this.status = res.status;
    this.body = res.response;
  });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
