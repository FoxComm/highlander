const _ = require('lodash');
const genDefaultConfig = require('@kadira/storybook/dist/server/config/defaults/webpack.config.js');
const { plugins } = require('../src/postcss.config');

module.exports = function(config, env) {
  const newConfig = genDefaultConfig(config, env);

  const loaders = newConfig.module.loaders;

  const cssLoader = _.find(loaders, loader => loader.test.toString() == '/\\.css?$/');
  cssLoader.loaders = [
    require.resolve('style-loader'),
    require.resolve('css-loader') + '?url=false&importLoaders=1',
    require.resolve('postcss-loader')
  ];

  newConfig.resolve.modulesDirectories = [
    'node_modules',
    'src'
  ];

  newConfig.postcss = () => plugins;

  return newConfig;
};
