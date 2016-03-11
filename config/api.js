'use strict';

module.exports = function(env) {
  const version = 'v1';

  function uri() {
    switch(env.environment) {
      default:
        return `${env.phoenix_url}/${version}`;
    }
  }

  function host() {
    switch(env.environment) {
      default:
        return env.phoenix_url;
    }
  }

  function auth() {
    return {
      header: 'JWT',
      cookieName: 'JWT',
      loginUri: '/login',
      publicKey: env.public_key,
    };

  }

  return {
    host: host(),
    uri: uri(),
    auth: auth(),
    version: version
  };
};
