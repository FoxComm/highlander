'use strict';

describe('Orders #PATCH', function() {
  it('should update an order status', function *() {
    let
      res     = yield this.api.patch('/orders/1', {orderStatus: 'remorseHold'}),
      order   = res.response;

    expect(res.status).to.equal(200);
    expect(order.id).to.equal('1');
    expect(order.orderStatus).to.equal('remorseHold');
  });
});
