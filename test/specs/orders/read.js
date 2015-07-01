'use strict';

describe('Orders #GET', function() {

  it('should find an order', function *() {
    let
      res     = yield this.api.get('/orders/1'),
      order   = res.response;
    expect(res.status).to.equal(200);
    expect(order.id).to.equal(1);
    expect(order.createdAt).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
    expect(order.referenceNumber).to.be.a('string');
    expect(order.orderStatus).to.be.a('string');
    expect(order.paymentStatus).to.be.a('string');
    expect(order).to.include.key('shippingStatus');
    expect(order.customer).to.be.a('object');
    expect(order.shippingAddress).to.be.a('object');
    expect(order.fraudScore).to.be.a('number');
    expect(order.totals.subTotal).to.be.a('number');
    expect(order.totals.shipping).to.be.a('number');
    expect(order.totals.taxes).to.be.a('number');
    expect(order.totals.adjustments).to.be.a('number');
    expect(order.totals.total).to.be.a('number');
    expect(order.lineItems).to.be.a('array');
    expect(order.payments).to.be.a('array');
  });

  it('should not find an order', function *() {
    try {
      yield this.api.get('/orders/9999999');
    } catch(err) {
      expect(err.status).to.equal(404);
      expect(err.response.error).to.equal('Cannot find Order');
    }
  });

  it('should get an array of orders', function *() {
    let
      res     = yield this.api.get('/orders'),
      orders  = res.response;
    expect(res.status).to.equal(200);
    expect(orders).to.have.length(50);
  });

  it('should get 20 orders', function *() {
    let
      res     = yield this.api.get('/orders', {limit: 20}),
      orders  = res.response;
    expect(res.status).to.equal(200);
    expect(orders).to.have.length(20);
  });

  it('should get a new page of orders', function *() {
    let
      res     = yield this.api.get('/orders', {page: 2}),
      orders  = res.response;
    expect(res.status).to.equal(200);
    expect(orders).to.have.length(50);
  });

  it('should get a new page of 10 orders', function *() {
    let
      res     = yield this.api.get('/orders', {limit: 10, page: 2}),
      orders  = res.response;
    expect(res.status).to.equal(200);
    expect(orders).to.have.length(10);
  });
});
