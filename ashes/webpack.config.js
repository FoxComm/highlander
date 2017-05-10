const path = require('path');
const webpack = require('webpack');
const merge = require('webpack-merge');

const isProduction = process.env.NODE_ENV === 'production';

const devConfig = require('./webpack/dev');
const prodConfig = require('./webpack/prod');

const cssLoader = {
  loader: 'css-loader',
  query: {
    modules: true,
    importLoaders: 1,
    localIdentName: '[name]__[local]'
  }
};

const baseConfig = {
  entry: [ 'babel-polyfill', './src/client.js' ],

  output: {
    path: path.resolve('./public/admin'),
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
        use: [ 'style-loader', 'css-loader', 'postcss-loader' ]
      },
      {
        test: /\.less$/,
        use: [ 'style-loader', 'css-loader', 'less-loader' ]
      },
      {
        test: /\.(woff|woff2)$/,
        use: [
          {
            loader: 'file-loader',
            query: {
              name: '[name].[ext]'
            }
          }
        ]
      }
    ]
  },

  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify(process.env.NODE_ENV)
      }
    }),
  ],

  resolve: {
    modules: [
      path.resolve(__dirname, 'node_modules'),
      path.resolve(__dirname, 'src')
    ],
    extensions: ['.js', '.jsx']
  },
};

module.exports = isProduction
  ? merge(baseConfig, prodConfig)
  : merge(baseConfig, devConfig);
