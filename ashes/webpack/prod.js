const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
// const WriteFilePlugin = require('write-file-webpack-plugin');
// const failPlugin = require('webpack-fail-plugin');
// const StatsPlugin = require('stats-webpack-plugin');
// const StatsWriterPlugin = require('webpack-stats-plugin').StatsWriterPlugin;

const SvgStore = require('webpack-svgstore-plugin');

module.exports = {
  entry: [
    'babel-polyfill',
    path.resolve(__dirname, '../src/client.js')
  ],

  output: {
    path: path.resolve(__dirname, '../build/admin'),
    filename: '[name].js',
  },

  module: {
    rules: [
      {
        test: /\.json$/,
        use: [ 'json-loader' ]
      },
      {
        test: /\.js(x)?$/,
        exclude: /node_modules/,
        use: [ 'babel-loader' ]
      },
      {
        test: /\.css$/,
        use: ExtractTextPlugin.extract({
          fallback: 'style-loader',
          use: [ 'css-loader', 'postcss-loader' ],
          allChunks: true,
        })
      },
      {
        test: /\.less$/,
        use: [ 'style-loader', 'css-loader', 'less-loader' ]
      },
    ]
  },

  plugins: [
    new ExtractTextPlugin('styles.css'),

    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify(process.env.NODE_ENV)
      }
    }),

    new SvgStore({}),
  ],

  resolve: {
    modules: [
      path.resolve(__dirname, '../node_modules'),
      path.resolve(__dirname, '../src')
    ],
    extensions: ['.js', '.jsx']
  },
};
