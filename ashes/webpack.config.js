const path = require('path');
const webpack = require('webpack');
const merge = require('webpack-merge');
const SvgStore = require('webpack-svgstore-plugin');

const isProduction = process.env.NODE_ENV === 'production';

const devConfig = require('./webpack/dev');
const prodConfig = require('./webpack/prod');

const baseConfig = {
  entry: [
    // 'babel-polyfill',
    path.resolve(__dirname, './src/client.js')
  ],

  output: {
    path: path.resolve(__dirname, './build/admin'),
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
          path.resolve(__dirname, './src'),
        ],
        use: [ 'babel-loader' ],
      },
      {
        test: /\.(png|woff|woff2)$/,
        use: [
          {
            loader: 'file-loader',
            query: {
              name: '[name].[ext]',
              publicPath: '/admin/'
            }
          }
        ]
      }
    ]
  },

  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify(process.env.NODE_ENV),
        BEHIND_NGINX: JSON.stringify(process.env.BEHIND_NGINX),
        GIT_REVISION: JSON.stringify(process.env.GIT_REVISION),
      },
    }),
    new webpack.optimize.CommonsChunkPlugin({
      filename:  '[name].js',
      name:      'vendor',
      minChunks: module => module.resource && module.resource.indexOf(path.resolve('node_modules')) >= 0,
    }),

    new SvgStore(),
  ],

  resolve: {
    modules: [
      path.resolve(__dirname, './node_modules'),
      path.resolve(__dirname, './src')
    ],
    extensions: ['.js', '.jsx']
  },
};

module.exports = isProduction
  ? merge(baseConfig, prodConfig)
  : merge(baseConfig, devConfig);
