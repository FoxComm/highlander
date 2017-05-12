const path = require('path');
const webpack = require('webpack');
const SvgStore = require('webpack-svgstore-plugin');

module.exports = {
  module: {
    rules: [
      {
        test: /\.css$/,
        use: [ 'style-loader', 'css-loader', 'postcss-loader' ]
      },
      {
        test: /\.less$/,
        use: [ 'style-loader', 'css-loader', 'less-loader' ]
      },
    ]
  },

  devtool: 'eval-source-map',
};
