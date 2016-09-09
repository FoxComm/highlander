'use strict';

let path = require('path');

// Simple dev / production switch
function _env(dev, production) {
  return process.env.BUILD_ENV === 'production' ? production : dev;
}

function _path(p) {
  return path.join(__dirname, p);
}

module.exports = {
  devtool: _env('eval', false),
  entry: _path('src/index.js'),
  output: {
    path: _path('lib'),
    filename: 'index.js',
    library: 'react-gestures',
    libraryTarget: 'commonjs2',
  },
  module: {
    loaders: [
      { test: /\.(js|jsx)$/, loader: 'babel?stage=0&optional=runtime' },
    ],
  },
  externals: {
    react: 'React',
  },
  resolve: {
    extensions: ['', '.react.js', '.js', '.jsx'],
  },
};
