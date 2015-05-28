'use strict';

module.exports = function(env) {
  const version = 'v1';

  function uri() {
    switch(env) {
      case 'test': return `http://localhost:3000/api/${version}`;
      default: return `http://private-5b93a4-foxcomm1.apiary-mock.com/`;
    }
  }
  return {
    uri: uri()
  }
}
