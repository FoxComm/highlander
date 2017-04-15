const path = require('path');
const postcssConfig = require('./../src/postcss');

module.exports = {
  entry: './src/client.js',
  devtool: 'eval-source-map',
  output: {
    path: path.resolve(__dirname, 'build'),
    filename: 'app.bundle.js',
  },
  resolve: {
    extensions: ['.js', '.jsx'],
    modules: [
      path.resolve('./src'),
      path.resolve('./node_modules')
    ],
    alias: {
      // 'rsg-components/Logo': path.join(__dirname, 'logo.jsx'),
    }
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: ['babel-loader'],
      },
      {
        // loader for global css located in src/css
        test: /\.css$/,
        include: [
          path.resolve(__dirname),
          path.resolve(__dirname, '../src/css'),
        ],
        use: ['style-loader', 'css-loader'],
      },
      {
        // loader for css-modules located in components declarations
        test: /\.css$/,
        exclude: [
          path.resolve(__dirname),
          path.resolve(__dirname, '../src/css'),
        ],
        use: [
          {
            loader: 'style-loader'
          },
          {
            loader: 'css-loader',
            options: { importLoaders: 1 }
          },
          {
            loader: 'postcss-loader',
            options: {
              // @see https://github.com/postcss/postcss-loader/issues/193#issuecomment-291563828
              // @see https://github.com/postcss/postcss-loader/issues/92#issuecomment-275696824
              ident: 'postcss',
              plugins: postcssConfig.plugins
            }
          },
        ]
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2|ico|jpg|jpeg|png|gif)$/,
        use: 'file-loader?name=public/[name].[ext]'
      },
    ]
  },
};
