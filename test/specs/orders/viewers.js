'use strict';

describe('Order Viewers #GET', function() {

  it('should get a list of viewers', function *() {
    let
      res     = yield this.api.get('/orders/1/viewers'),
      viewers = res.response;
    expect(res.status).to.equal(200);
    expect(viewers).to.have.length.above(1);
  });
});
