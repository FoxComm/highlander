const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const OptimizeCssAssetsPlugin = require('optimize-css-assets-webpack-plugin');

module.exports = {
  output: {
    filename: '[name].[chunkhash:6].js'
  },

  module: {
    rules: [
      {
        test: /\.jsx?$/,
        include: [
          path.resolve(__dirname, '../src'),
        ],
        use: [ 'babel-loader' ],
      },
      {
        test: /\.css$/,
        use: ExtractTextPlugin.extract({
          use: [ 'css-loader', 'postcss-loader' ]
        })
      },
      {
        test: /\.less$/,
        use: ExtractTextPlugin.extract({
          use: [ 'css-loader', 'less-loader' ]
        })
      },
    ]
  },

  plugins: [
    // Move all node_modules to `vendor.js` chunk
    new webpack.optimize.CommonsChunkPlugin({
      name: 'vendor',
      minChunks: module => module.resource && module.resource.includes(path.resolve('node_modules')),
    }),

    // Move all chunkhashes to manifest chunk https://webpack.js.org/guides/code-splitting-libraries/#manifest-file
    new webpack.optimize.CommonsChunkPlugin({
      name: 'manifest'
    }),

    new webpack.optimize.UglifyJsPlugin({
      compress: { warnings: false },
      sourceMap: false
    }),

    new ExtractTextPlugin('app.[contenthash:6].css'),
    new OptimizeCssAssetsPlugin(),
  ],

  // https://github.com/webpack-contrib/extract-text-webpack-plugin/issues/35
  stats: {
    children: false
  },
};
