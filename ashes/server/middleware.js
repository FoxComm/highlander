const fs = require('fs');
const path = require('path');
const _ = require('lodash');
const jwt = require('jsonwebtoken');

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
        console.info('token.roles doesn\'t contain admin role', token.roles);
        return null; // only admins allowed to proceed
      }

      return token;
    } catch(err) {
      console.warn(`Can't decode token: ${err}`);
    }
  }

  app.verifyToken = function *(next) {
    this.state.token = getToken(this);

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
    const layoutData = _.defaults({
      tokenOk: !!this.state.token,
      stylesheet: `/admin/admin.css`,
      javascript: `/admin/main.js`,
      // use GA_LOCAL=1 gulp dev command for enable tracking events in google analytics from localhost
      gaEnableLocal: 'GA_LOCAL' in process.env,
      JWT: JSON.stringify(this.state.jwt || null),
      stripeApiKey: JSON.stringify(process.env.STRIPE_PUBLISHABLE_KEY || null),
    }, config.layout.pageConstants);

    this.body = layout(layoutData);
  };
};
