'use strict';

const
  path = require('path');

module.exports = function() {
  return {
    favicon: path.resolve('public/favicon.ico'),
    publicDir: path.resolve('public'),
    pageConstants: {
      title: 'FoxCommerce | Ashes',
      analytics: 'UA-XXXXXXXX-1',
      appStart: 'App.start();'
    }
  };
};
