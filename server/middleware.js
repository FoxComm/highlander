'use strict';

const
  fs          = require('fs'),
  path        = require('path'),
  _           = require('underscore'),
  htmlescape  = require('htmlescape'),
  errors      = require('./errors');

module.exports = function(app) {
  const
    config    = app.config,
    template  = path.join(__dirname, './views/layout.tmpl'),
    layout    = _.template(fs.readFileSync(template, 'utf8'));

  app.requireUser = function *(next) {
    if (!this.currentUser) {
      throw new errors.Unauthorized();
    }
    yield next;
  };

  app.requireAdmin = function *(next) {
    if (!this.currentUser || !this.currentUser.isAdmin) {
      throw new errors.Unauthorized();
    }
    yield next;
  };

  app.jsonError = function *(next) {
    try {
      yield next;
    } catch(err) {
      this.status = err.status || 500;
      this.body = {error: err.message};
    }
  };

  app.renderReact = function *() {
    const appFile = path.join(config.layout.publicDir, 'themes', this.theme, `${this.theme}.js`);
    const App     = require(appFile);

    let bootstrap = {
      path: this.path
    };

    let layoutData = _.defaults({
      stylesheet: `/themes/${this.theme}/${this.theme}.css`,
      javascript: `/themes/${this.theme}/${this.theme}.js`,
      rootHTML: yield App.start(bootstrap),
      appStart: `App.start(${htmlescape(bootstrap)});`
    }, config.layout.pageConstants);

    this.body = layout(layoutData);
  };
};
