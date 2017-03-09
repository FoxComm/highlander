const fs = require('fs');
const path = require('path');
const _ = require('lodash');
const jwt = require('jsonwebtoken');
require('babel-polyfill');

const htmlescape = require('htmlescape');
const errors = require('./errors');

const { isPathRequiredAuth } = require('../lib/route-rules');

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
  const sprite = fs.readFileSync(path.resolve('build/svg/fc-sprite.svg'), 'utf-8');

  // lets do renderReact property is lazy
  Object.defineProperty(app, 'renderReact', {
    get: function() {
      return require('../lib/render').renderReact;
    }
  });

  function getToken(ctx) {
    const jwtToken = ctx.cookies.get(config.api.auth.cookieName);
    if (!jwtToken) {
      return null;
    }
    ctx.state.jwt = jwtToken;
    try {
      let token;
      if (process.env.DEV_SKIP_JWT_VERIFY) {
        console.info('DEV_SKIP_JWT_VERIFY is enabled, JWT is not verified');
        token = jwt.decode(jwtToken);
      } else {
        token = jwt.verify(jwtToken, loadPublicKey(config), {
          issuer: 'FC',
          audience: 'user',
          algorithms: ['RS256', 'RS384', 'RS512']
        });
      }
      if (!_.includes(token.roles, 'admin')) {
        console.log('token.roles doesn\'t contain admin role', token.roles);
        return null; // only admins allowed to proceed
      }
      return token;
    }
    catch(err) {
      console.warn(`Can't decode token: ${err}`);
    }
  }

  app.requireAdmin = function *(next) {
    if (isPathRequiredAuth(this.request.path)) {
      const token = getToken(this);
      // TODO: When we read tokens, validate that we have a claim to the admin UI.
      if (!token) {
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
      console.error(err);

      let body;

      if (err.stack && this.env !== 'production') {
        body = {errors: [err.stack], env: 'node'};
      } else {
        body = {errors: [err.message ? err.message : String(err)], env: 'node'};
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
      fcsprite: sprite,
      rootHTML: this.state.html,
      appStart: `App.start(${htmlescape(bootstrap)});`,
      // use GA_LOCAL=1 gulp dev command for enable tracking events in google analytics from localhost
      gaEnableLocal: 'GA_LOCAL' in process.env,
      JWT: JSON.stringify(this.state.jwt || null),
      stripeApiKey: JSON.stringify(process.env.STRIPE_PUBLISHABLE_KEY || null),
    }, config.layout.pageConstants);

    this.body = layout(layoutData);
  };
};
