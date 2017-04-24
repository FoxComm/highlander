const path = require('path');

module.exports = {
  title: 'Ashes Components Style Guide',
  showCode: true,
  components: '../src/components/core/**/*.jsx',
  webpackConfig: require('./webpack.styleguide.js'),
  styleguideDir: path.resolve('public/admin/styleguide'),
  require: [
    path.join(__dirname, 'styleguide.css'),
    path.join(__dirname, './css/app.css'),
  ]
};