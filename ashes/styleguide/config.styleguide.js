const path = require('path');
const _ = require('lodash');

module.exports = {
  title: 'Ashes Components Style Guide',
  showCode: true,
  components: '../src/components/core/**/*.jsx',
  webpackConfig: require('./webpack.styleguide.js'),
  require: [
    path.join(__dirname, 'styleguide.css'),
    path.join(__dirname, '../src/css/app.css'),
  ]
};
