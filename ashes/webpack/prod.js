const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const OptimizeCssAssetsPlugin = require('optimize-css-assets-webpack-plugin');
const ManifestPlugin = require('webpack-manifest-plugin');

module.exports = {
  output: {
    // What is chunkhash? Read explanation: https://github.com/webpack-contrib/extract-text-webpack-plugin/issues/153
    filename: '[name].[chunkhash:4].js',

    // We set const string here because `chunkFilename` value will be injected to vendor.js.
    // It expected to be vice versa, but for some reasons webpack thinks that `vendor.js` is the main chunk.
    // Default of `chunkFilename` is something like `[name].[chunkhash].js`, and if `main.js` changes, its hash changes,
    // which changes the content of `vendor.js`, which changes hash of `vendor.js`,
    // and that breaks the point of long-term cache.
    // If we want `true` (not hardcoded) chunks, `chunkFilename` must be changed.
    chunkFilename: 'unused-but-const-string'
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

    new webpack.optimize.UglifyJsPlugin({
      compress: { warnings: false },
      sourceMap: false
    }),

    new ExtractTextPlugin('app.[contenthash:4].css'),
    new OptimizeCssAssetsPlugin(),

    new ManifestPlugin(),
  ],

  // https://github.com/webpack-contrib/extract-text-webpack-plugin/issues/35
  stats: {
    children: false
  },
};
