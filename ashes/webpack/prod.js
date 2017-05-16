const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const OptimizeCssAssetsPlugin = require('optimize-css-assets-webpack-plugin');

module.exports = {
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
    new webpack.optimize.UglifyJsPlugin({
      compress: { warnings: false },
      sourceMap: false
    }),

    new webpack.optimize.CommonsChunkPlugin({
      filename: '[name].js',
      name: 'vendor',
      minChunks: module => module.resource && module.resource.indexOf(path.resolve('node_modules')) >= 0,
    }),

    new ExtractTextPlugin('styles.css'),
    new OptimizeCssAssetsPlugin(),
  ],

  // https://github.com/webpack-contrib/extract-text-webpack-plugin/issues/35
  stats: {
    children: false
  },
};
