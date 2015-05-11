'use strict';

const
  Router      = require('koa-router'),
  fs          = require('fs'),
  path        = require('path'),
  _           = require('underscore'),
  htmlescape  = require('htmlescape');

module.exports = function(app) {
  let
    config    = app.config,
    router    = new Router(),
    template  = path.join(__dirname, '../views/layout.tmpl');

  app.layout = _.template(fs.readFileSync(template, 'utf8'));

  router
    .get('/ping', function *() {
      this.status = 200;
      this.body = 'OK';
    })
    .get('/:path*', function *() {
      let
        theme   = this.query.theme || config.layout.theme,
        appFile = path.join(config.layout.publicDir, 'themes', theme, `${theme}.js`);

      let App = require(appFile);

      let bootstrap = {
        path: this.path
      };

      let layoutData = _.defaults({
        stylesheet: `/themes/${theme}/${theme}.css`,
        javascript: `/themes/${theme}/${theme}.js`,
        rootHTML: yield App.start(bootstrap),
        appStart: `App.start(${htmlescape(bootstrap)});`
      }, config.layout.pageConstants);

      this.body = app.layout(layoutData);
    });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
