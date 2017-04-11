
process.env.API_URL = process.env.API_URL || 'http://localhost';
process.env.NODE_ENV = process.env.NODE_ENV || 'development';
process.env.STOREFRONT_LANGUAGE = process.env.STOREFRONT_LANGUAGE || 'en';
process.env.URL_PREFIX = process.env.URL_PREFIX || '';
process.env.STOREFRONT_CONTEXT = process.env.STOREFRONT_CONTEXT || 'default';

require('./setup_node_path');

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
