
const _ = require('lodash');
const path = require('path');
const { fork } = require('child_process');

const libDir = path.resolve(path.join(__dirname, '../lib'));
process.env.NODE_PATH = _.compact([process.env.NODE_PATH, libDir]).join(':');

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

fork(path.join(__dirname, 'instance.js'));
