'use strict';

module.exports = function(env) {
  const version = 'v1';

  function uri() {
    switch(env.environment) {
      case 'apiary': return 'http://private-5b93a4-foxcomm1.apiary-mock.com/';
      default: return `${env.phoenix_url}/${version}`;
    }
  }

  function auth() {
    switch(env.environment) {
      case 'staging': return {user: 'admin@admin.com', password: 'password' };
      default: return {user: 'admin@admin.com', password: 'password'};
    }
  }

  return {
    uri: uri(),
    auth: auth(),
    version: version
  };
};
