const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const OptimizeCssAssetsPlugin = require('optimize-css-assets-webpack-plugin');
const ManifestPlugin = require('webpack-manifest-plugin');

module.exports = {
  output: {
    filename: 'app.[hash:6].js',
    sourceMapFilename: '[name].map'
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
    new webpack.optimize.CommonsChunkPlugin({
      filename: 'vendor.[hash:6].js',
      name: 'vendor',
      minChunks: module => module.resource && module.resource.indexOf(path.resolve('node_modules')) >= 0,
    }),

    new webpack.optimize.UglifyJsPlugin({
      compress: { warnings: false },
      sourceMap: false
    }),

    new ExtractTextPlugin('app.[contenthash:6].css'),
    new OptimizeCssAssetsPlugin(),

    new ManifestPlugin(),
  ],

  // https://github.com/webpack-contrib/extract-text-webpack-plugin/issues/35
  stats: {
    children: false
  },
};
