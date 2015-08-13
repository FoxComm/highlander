'use strict';

module.exports = function(env) {
  const version = 'v1';

  function uri() {
    switch(env) {
      case 'test': return `http://localhost:3001/fauxnix/${version}`;
      case 'apiary': return 'http://private-5b93a4-foxcomm1.apiary-mock.com/';
      case 'phoenix': return `http://localhost:9090/${version}`;
      case 'staging': return `http://10.240.230.233:9090/${version}`;
      default: return `http://localhost:4000/fauxnix/${version}`;
    }
  }

  function auth() {
    switch(env) {
      case 'phoenix': return {user: 'admin@admin.com', password: 'password' };
      default: return null;
    }
  }

  return {
    uri: uri(),
    auth: auth(),
    version: version
  };
};
