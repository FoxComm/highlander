
const assert = require('assert');
const nock = require('nock');
const Api = require('../lib/index').default;

function getApi() {
  return new Api({
    api_url: 'http://api.foxcommerce',
    stripe_key: 'not_used_here',
  });
}

describe('auth', function() {
  const api = getApi();

  it('login', function() {
    const scope = nock('http://api.foxcommerce')
      .post('/v1/public/login')
      .reply(200, {result: 'ok'});


    api.auth.login('admin@admin.com', 'password', 'customer');
    scope.done();
  });
});
