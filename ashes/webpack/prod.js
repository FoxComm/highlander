const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const OptimizeCssAssetsPlugin = require('optimize-css-assets-webpack-plugin');
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
        include: [
          path.resolve(__dirname, "../src"),
          // @todo
          // path.resolve(__dirname, "../node_modules/react"),
          // path.resolve(__dirname, "../node_modules/react-imgix"),
        ],
        use: [ 'babel-loader' ],
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
    new OptimizeCssAssetsPlugin(),

    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify(process.env.NODE_ENV),
        BEHIND_NGINX: JSON.stringify(process.env.BEHIND_NGINX),
        GIT_REVISION: JSON.stringify(process.env.GIT_REVISION),
      },
    }),

    new SvgStore(),
  ],

  resolve: {
    modules: [
      path.resolve(__dirname, '../node_modules'),
      path.resolve(__dirname, '../src')
    ],
    extensions: ['.js', '.jsx']
  },

  // https://github.com/webpack-contrib/extract-text-webpack-plugin/issues/35
  stats: {
    children: false
  },
};
