'use strict';

describe('Orders #GET', function() {

  it('should find an order', function *() {
    let
      res     = yield this.Api.get('/api/v1/orders/1'),
      status  = res[0].statusCode,
      order   = res[1];
    expect(status).to.equal(200);
    expect(order.orderId).to.be.a.string;
    expect(order.orderStatus).to.be.a.string;
    expect(order.paymentStatus).to.be.a.string;
    expect(order.shippingStatus).to.be.a.string;
    expect(order.customer).to.be.a.object;
    expect(order.total).to.be.a.number;
  });

  it('should get an array of orders', function *() {
    let
      res     = yield this.Api.get('/api/v1/orders'),
      status  = res[0].statusCode,
      orders  = res[1];
    expect(status).to.equal(200);
    expect(orders).to.have.length(50);
  });
});
