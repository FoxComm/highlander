const path = require('path');

process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./src/core')}`;

require('babel-register')();
require('./src/postcss').installHook();

const makeRoutes = require('./src/routes').default;
const Sitemap = require('react-router-sitemap').default;
const {
  categories,
  productTypes,
  convertCategoryNameToUrlPart,
} = require('modules/categories');

const routes = makeRoutes();
const categoryNames = categories.map(c => convertCategoryNameToUrlPart(c.name));

const paramsConfig = {
  '/:categoryName(/:productType)': [
    {
      categoryName: encodeURIComponent('ENTRÉES'),
      productType: productTypes,
    },
    {
      categoryName: categoryNames,
      productType: '',
    },
  ],
};

const excludedRoutes = {
  isValid: false,
  rules: [/\/?auth/, /\/profile/, /\/products/, /\/search/, /\/checkout/],
};

new Sitemap(routes)
  .filterPaths(excludedRoutes)
  .applyParams(paramsConfig)
  .build('https://theperfectgourmet.com')
  .save('./public/sitemap.xml');
