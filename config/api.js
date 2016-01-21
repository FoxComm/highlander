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
    switch(env.environment) {
      case 'staging':
        return {user: 'admin@admin.com', password: 'password' };
      default:
        return {user: 'admin@admin.com', password: 'password'};
    }
  }

  return {
    host: host(),
    uri: uri(),
    auth: auth(),
    version: version
  };
};
