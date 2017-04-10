
const path = require('path');
const _ = require('lodash');

process.env.API_URL = process.env.API_URL || 'http://localhost';
process.env.NODE_ENV = process.env.NODE_ENV || 'development';
process.env.STOREFRONT_LANGUAGE = process.env.STOREFRONT_LANGUAGE || 'en';
process.env.URL_PREFIX = process.env.URL_PREFIX || '';
process.env.STOREFRONT_CONTEXT = process.env.STOREFRONT_CONTEXT || 'default';

const storefrontRoot = path.normalize(path.resolve(
  path.join(__dirname, '..')
));

const libDir = path.join(storefrontRoot, 'lib');
let additionalPaths = [libDir];
if (process.cwd() != storefrontRoot) {
  additionalPaths = [path.join(process.cwd(), 'lib'), ...additionalPaths];
}
process.env.NODE_PATH = _.compact([process.env.NODE_PATH, ...additionalPaths]).join(':');

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
