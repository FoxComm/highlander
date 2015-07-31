'use strict';

const
  path = require('path');

module.exports = function(env) {
  function port() {
    switch(env) {
      case 'test': return 3001;
      default: return process.env.PORT || 4000;
    }
  }

  return {
    port: port(),
    publicDir: path.resolve('public')
  };
};
