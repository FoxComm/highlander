const path = require('path');
const webpack = require('webpack');
const merge = require('webpack-merge');
const SvgStore = require('webpack-svgstore-plugin');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

const isProduction = process.env.NODE_ENV === 'production';

const devConfig = require('./webpack/dev');
const prodConfig = require('./webpack/prod');

const baseConfig = {
  entry: [
    path.resolve(__dirname, './src/client.js')
  ],

  output: {
    path: path.resolve(__dirname, './build/admin'),
    publicPath: '/admin/',
    filename: '[name].js',
  },

  module: {
    rules: [
      {
        test: /\.json$/,
        use: [ 'json-loader' ]
      },
      {
        test: /\.(png|ico|woff|woff2)$/,
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
    new webpack.EnvironmentPlugin({
      NODE_ENV: 'development',
      BEHIND_NGINX: false,
      GIT_REVISION: 'unknown'
    }),

    new SvgStore(),

    // Uncomment it and relaunch (make p or make d): stats will be opened in your default browser
    // new BundleAnalyzerPlugin(),
  ],

  resolve: {
    extensions: ['.js', '.jsx']
  },
};

module.exports = isProduction
  ? merge(baseConfig, prodConfig)
  : merge(baseConfig, devConfig);
