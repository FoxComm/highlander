const genDefaultConfig = require('@kadira/storybook/dist/server/config/defaults/webpack.config.js');
const { plugins } = require('../src/postcss.config');

module.exports = function(config, env) {
  const newConfig = genDefaultConfig(config, env);

  newConfig.postcss = () => plugins;

  return newConfig;
};
