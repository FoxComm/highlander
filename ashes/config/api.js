'use strict';

module.exports = function(env) {
  return {
    auth: {
      header: 'JWT',
      cookieName: 'JWT',
      publicKey: env.public_key,
    },
  };
};
