'use strict';

describe('Orders Viewers #GET', function() {

  it('should get a list of viewers', function *() {
    let
      res     = yield this.Api.get('/api/v1/orders/1/viewers'),
      status  = res[0].statusCode,
      viewers = res[1];
    expect(status).to.equal(200);
    expect(viewers).to.have.length.above(1);
  });
});
