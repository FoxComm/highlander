const path = require('path');
const Router = require('koa-router');
const serve = require('koa-better-static');

function *styleguide(next) {
  const matchUriRegexp = new RegExp(`/styleguide/`);
  const match = matchUriRegexp.exec(this.request.url);

  if (match) {
    const root = this.path.substr(0, match.index);

    this.path = this.path.substr(match.index);

    const srv = serve(path.resolve(__dirname, '../../styleguide'), { index: 'index.html' });

    return yield srv.call(this, next);
  }

  yield next;
}

module.exports = function (app) {
  const router = new Router();

  router
    .get('/:path*', app.renderReact, app.renderLayout);

  app
    .use(styleguide)
    .use(app.requireAdmin)
    .use(router.routes())
    .use(router.allowedMethods());
};
