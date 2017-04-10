const path = require('path');
const postcssConfig = require('../src/postcss');

module.exports = {
  module: {
    loaders: [
      {
        test: /\.css$/,
        loaders: ['style', 'css?importLoaders=1', 'postcss']
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2)$/,
        loader: 'file?name=public/fonts/[name].[ext]'
      },
      {
        test: /\.(ico|jpg|jpeg|png|gif|svg)$/,
        loader: 'file',
      },
    ]
  },
  postcss: () => {
    return postcssConfig.plugins;
  }
};
