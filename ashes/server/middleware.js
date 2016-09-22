const fs = require('fs');
const path = require('path');
const _ = require('lodash');
const jwt = require('jsonwebtoken');
require('babel-polyfill');

const htmlescape = require('htmlescape');
const errors = require('./errors');

function loadPublicKey(config) {
  try {
    return fs.readFileSync(config.api.auth.publicKey);
  }
  catch (err) {
    console.error(err);
    throw `Can't load public key ${config.api.auth.publicKey}, exit`;
  }
}

module.exports = function(app) {
  const config = app.config;
  const template = path.join(__dirname, './views/layout.tmpl');
  const layout = _.template(fs.readFileSync(template, 'utf8'));


  // lets do renderReact property is lazy
  Object.defineProperty(app, 'renderReact', {
    get: function() {
      return require('../src/render').renderReact;
    }
  });

  const publicKey = loadPublicKey(config);

  function getToken(ctx) {
    const token = ctx.cookies.get(config.api.auth.cookieName);
    if (!token) {
      return null;
    }
    try {
      return jwt.verify(token, publicKey, {issuer: 'FC', audience: 'admin', algorithms: ['RS256', 'RS384', 'RS512']});
    }
    catch(err) {
      console.error(`Can't decode token: ${err}`);
    }
  }

  app.requireAdmin = function *(next) {
    const authFreeUrls = /\/(login|signup)$/;
    if (!this.request.path.match(authFreeUrls)) {
      const token = getToken(this);
      if (!token || !token.admin) {
        this.redirect(config.api.auth.loginUri);
      }
      this.state.token = token;
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
      stylesheet: `/admin/admin.css`,
      javascript: `/admin/admin.js`,
      rootHTML: this.state.html,
      appStart: `App.start(${htmlescape(bootstrap)});`,
      // use GA_LOCAL=1 gulp dev command for enable tracking events in google analytics from localhost
      gaEnableLocal: 'GA_LOCAL' in process.env,
    }, config.layout.pageConstants);

    this.body = layout(layoutData);
  };
};
