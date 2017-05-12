const path = require('path');
const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const OptimizeCssAssetsPlugin = require('optimize-css-assets-webpack-plugin');

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
      compress: { warnings: false }
    }),

    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify(process.env.NODE_ENV),
        BEHIND_NGINX: JSON.stringify(process.env.BEHIND_NGINX),
        GIT_REVISION: JSON.stringify(process.env.GIT_REVISION),
      },
    }),

    new ExtractTextPlugin('styles.css'),
    new OptimizeCssAssetsPlugin(),

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
