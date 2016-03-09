const fs = require('fs');
const path = require('path');
const _ = require('lodash');
const jwt = require('jsonwebtoken');

const htmlescape = require('htmlescape');
const errors = require('./errors');

module.exports = function(app) {
  const config = app.config;
  const template = path.join(__dirname, './views/layout.tmpl');
  const layout = _.template(fs.readFileSync(template, 'utf8'));

  const cert = fs.readFileSync(config.api.auth.publicCert);

  // lets do renderReact property is lazy
  Object.defineProperty(app, 'renderReact', {
    get: function() {
      const App = require('../src/app');

      return App.renderReact;
    }
  });

  function getToken(ctx) {
    const token = ctx.cookies.get(config.api.auth.cookieName);
    if (!token) {
      return null;
    }
    try {
      return jwt.verify(token, cert, {issuer: "FC", subject: "site", algorithms: ['RS256', 'RS384', 'RS512']});
    }
    catch(err) {
      console.error("Can't decode token: ", err);
    }
  }

  app.requireAdmin = function *(next) {
    if (!this.request.url.match(config.api.auth.loginUri)) {
      const token = getToken(this);
      if (!token || !token.admin) {
        this.redirect(config.api.auth.loginUri);
      }
    }

    yield next;
  };

  app.jsonError = function *(next) {
    try {
      yield next;
    } catch(err) {
      this.status = err.status || 500;

      let body;

      if (err.stack && this.env !== 'production') {
        body = {error: err.stack};
      } else {
        body = {error: err.message ? err.message : String(err)};
      }
      this.body = body;
    }
  };

  app.renderLayout = function *() {
    let bootstrap = {
      path: this.path
    };

    let layoutData = _.defaults({
      stylesheet: `/admin.css`,
      javascript: `/admin.js`,
      rootHTML: this.state.html,
      appStart: `App.start(${htmlescape(bootstrap)});`
    }, config.layout.pageConstants);

    this.body = layout(layoutData);
  };
};
