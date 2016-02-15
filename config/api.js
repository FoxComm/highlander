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
      header: 'X-JWT',
      secret: 'adfm103ka09jsgo;sihd985ht209j;aoshg;osznfb;kzjdfhg0923jrt0wiejfaskfpaefmos%*rng3',
      loginUri: '/api/v1/setuser',
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
