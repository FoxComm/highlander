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

  // lets do renderReact property is lazy
  Object.defineProperty(app, 'renderReact', {
    get: function() {
      const App = require('../src/app');

      return App.renderReact;
    }
  });

  function getToken(request) {
    const token = request.get(config.api.auth.header);
    if (!token) {
      console.log('token not found!');
      return null;
    }
    try {
      console.log('verify token');
      return jwt.verify(token, config.api.auth.secret);
    }
    catch(err) {
      console.error("Can't decode token: ", err);
    }
  }

  app.requireAdmin = function *(next) {
    const token = getToken(this.request);
    if (!token || !token.admin) {
      if (!this.request.url.match(config.api.auth.loginUri)) {
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
