const path = require('path');
const webpack = require('webpack');
const SvgStore = require('webpack-svgstore-plugin');

module.exports = {
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        exclude: /node_modules/,
        use: ['babel-loader'],
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader', 'postcss-loader']
      },
      {
        test: /\.less$/,
        use: ['style-loader', 'css-loader', 'less-loader'],
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2|ico|jpg|jpeg|png|gif)$/,
        use: 'file-loader?name=public/[name].[ext]'
      },
    ]
  },

  resolve: {
    alias: {
      'rsg-components/Logo': path.join(__dirname, 'rsg-components/Logo')
    }
  },

  plugins: [
    new webpack.EnvironmentPlugin({
      STYLEGUIDE: true
    }),

    new SvgStore(),
  ],
};
