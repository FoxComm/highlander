const path = require('path');
const webpack = require('webpack');
const SvgStore = require('webpack-svgstore-plugin');

module.exports = {
  entry: [
    'babel-polyfill',
    path.resolve(__dirname, '../src/client.js')
  ],

  output: {
    path: path.resolve(__dirname, '../build'),
    filename: 'admin/[name].js',
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
        test: /\.(png|woff|woff2)$/,
        use: [
          {
            loader: 'file-loader',
            query: {
              name: '/admin/[name].[ext]'
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

    new SvgStore({}),
  ],

  resolve: {
    modules: [
      path.resolve(__dirname, '../node_modules'),
      path.resolve(__dirname, '../src')
    ],
    extensions: ['.js', '.jsx']
  },

  devtool: 'eval-source-map',
};
