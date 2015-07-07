'use strict';

describe('Search #GET', function() {
  it('should get a list of orders', function *() {
    let data = {
      q: 'email:jones',
      size: 5
    };

    let
      res = yield this.api.get('/search', data),
      orders = res.response;
    expect(res.status).to.equal(200);
    expect(orders).to.have.length(5);
  });
});
