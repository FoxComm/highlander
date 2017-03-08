
const path = require('path');

process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./lib')}`;

require('../src/postcss').installHook();
require('./env_defaults');

if (!process.env.GA_TRACKING_ID) {
  console.warn(
    'WARNING. There is no google analytics tracking id configured.' +
    'Use GA_TRACKING_ID env variable for that.'
  );
}

if (process.env.NODE_ENV == 'production' &&
  (process.env.MAILCHIMP_API_KEY === undefined ||
  process.env.CONTACT_EMAIL === undefined)) {
  throw new Error(
    'MAILCHIMP_API_KEY and CONTACT_EMAIL variables should be defined in environment.'
  );
}

const App = require('./app');

process.title = 'tpg-ui';

const app = new App();
app.start();
