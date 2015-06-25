'use strict';

const
  Router  = require('koa-router'),
  parse   = require('co-body');

module.exports = function(app) {
  let router = new Router();

  // @todo this will be reenabled once auth is available
  // - Tivs
  // router.use(app.requireAdmin);

  router
    .post('/gitup', function *() {
      if (app.env === 'production') {
        this.status = 400;
        return;
      }

      let
        hub       = this.get('X-Hub-Signature').split('='),
        algo      = hub[0],
        signature = hub[1],
        body      = yield parse.json(this);

      let hash = require('crypto')
        .createHmac(algo, 'foxcomm')
        .update(JSON.stringify(body))
        .digest('hex');
      if (hash !== signature) {
        this.status = 400;
        return;
      }

      let spawn = require('child_process').spawn;
      spawn('git', ['pull']);
      this.status = 200;
    })
    .get('/:path*', function *(next) {
      this.theme = 'admin';
      yield next;
    }, app.renderReact);

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
