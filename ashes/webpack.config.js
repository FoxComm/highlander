const path = require('path');
const webpack = require('webpack');
const merge = require('webpack-merge');

const isProduction = process.env.NODE_ENV === 'production';

const devConfig = require('./webpack/dev');
const prodConfig = require('./webpack/prod');

const cssLoader = {
  loader: 'css-loader',
  query: {
    modules: true,
    importLoaders: 1,
    localIdentName: '[name]__[local]'
  }
};

const baseConfig = {};

module.exports = isProduction
  ? merge(baseConfig, prodConfig)
  : merge(baseConfig, devConfig);
