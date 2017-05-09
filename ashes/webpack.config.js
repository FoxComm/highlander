var path = require('path');
var webpack = require('webpack');

var cssLoader = {
  loader: 'css-loader',
  query: {
    modules: true,
    importLoaders: 1,
    localIdentName: '[name]__[local]'
  }
};

module.exports = {
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

  // devServer: {
  //   outputPath: path.resolve(__dirname, '..', 'public'),
  //   contentBase: path.resolve(__dirname, '..', 'public'),
  //   watch: true,
  //   stats: 'errors-only' // https://github.com/webpack/webpack/issues/1191#issuecomment-157148084
  // }
};
