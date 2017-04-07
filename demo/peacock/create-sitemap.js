const path = require('path');

process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./lib')}`;

require('./src/postcss.config').installHook();

const makeRoutes = require('./lib/routes').default;
const Sitemap = require('react-router-sitemap').default;

const routes = makeRoutes();

const excludedRoutes = {
  isValid: false,
  rules: [/\/?auth/, /\/profile/, /\/products/, /\/search/, /\/checkout/],
};

new Sitemap(routes)
  .filterPaths(excludedRoutes)
  .build('https://demo.foxcommerce.com')
  .save('./public/sitemap.xml');
