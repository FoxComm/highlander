const Router  = require('koa-router');

module.exports = function(app) {
  const router = new Router();

  router
    .get('/:path*', app.renderLayout);

  app
    .use(app.requireAdmin)
    .use(router.routes())
    .use(router.allowedMethods());
};
