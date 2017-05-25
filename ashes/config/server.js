'use strict';

module.exports = function(env) {
  function port() {
    switch(env.environment) {
      case 'test': return 3001;
      default: return process.env.PORT;
    }
  }

  return {
    port: port()
  };
};
