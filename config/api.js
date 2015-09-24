'use strict';

module.exports = function(env) {
  const version = 'v1';

  function uri() {
    switch(env) {
      case 'apiary': return 'http://private-5b93a4-foxcomm1.apiary-mock.com/';
      case 'staging': return `http://10.240.230.233:9090/${version}`;
      case 'production': return `http://10.240.230.233:9090/${version}`;
      default: return `http://localhost:9090/${version}`;
    }
  }

  function auth() {
    switch(env) {
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
