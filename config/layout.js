'use strict';

const
  path = require('path');

module.exports = function(env) {
  return {
    theme: 'fluid',
    favicon: path.resolve('public/favicon.ico'),
    publicDir: path.resolve('public'),
    pageConstants: {
      title: 'Ashes',
      analytics: 'UA-XXXXXXXX-1',
      appStart: 'App.start();'
    }
  }
}
