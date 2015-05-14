'use strict';

const
  Router = require('koa-router');

module.exports = function(app) {
  const
    config  = app.config,
    router  = new Router();

  router
    .get('/ping', function *() {
      this.status = 200;
      this.body = 'OK';
    })
    .get('/:path*', function *(next) {
      this.theme = this.query.theme || config.layout.theme;
      yield next;
    }, app.renderReact);

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
