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
    const auth = {
      header: 'JWT',
      cookieName: 'JWT',
      loginUri: '/login',
      publicCert: '/tmp/public_key.pem'
    };

    return auth;
  }

  return {
    host: host(),
    uri: uri(),
    auth: auth(),
    version: version
  };
};
