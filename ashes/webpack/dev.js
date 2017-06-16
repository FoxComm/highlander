const path = require('path');

module.exports = {
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        include: [
          path.resolve(__dirname, '../src'),
        ],
        use: [ 'babel-loader?cacheDirectory=true' ],
      },
      {
        test: /\.css$/,
        use: [ 'style-loader', 'css-loader?{sourceMap:true}', 'postcss-loader' ],
      },
      {
        test: /\.less$/,
        use: [ 'style-loader', 'css-loader?{sourceMap:true}', 'less-loader' ]
      },
    ]
  },

  devtool: 'eval-source-map',
};
