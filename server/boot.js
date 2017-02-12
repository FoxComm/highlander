
const path = require('path');

process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./src/core')}`;

require('babel-register')();
require('../src/postcss').installHook();
require('./env_defaults');

if (process.env.NODE_ENV == 'production' &&
  (process.env.MAILCHIMP_API_KEY === undefined ||
  process.env.CONTACT_EMAIL === undefined)) {
  throw new Error(
    'MAILCHIMP_API_KEY and CONTACT_EMAIL variables should be defined in environment.'
  );
}

const App = require('./app').default;

process.title = 'tpg-ui';

const app = new App();
app.start();
