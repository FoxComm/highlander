'use strict';

describe('Order addresses change', function() {

  let addressId;

  it('should get a order with shippingAddress', function *() {
    let res = yield this.api.get('/orders/1');
    expect(res.status).to.equal(200);

    addressId = res.response.shippingAddress.id;
  });

  it('should change shipping address for order', function *() {
    // for real api we can post full address body instead of id
    let res = yield this.api.post('/orders/1/shipping-address', {addressId: addressId + 1});

    expect(res.status).to.equal(200);
  });

  it('order should have new address after change', function *() {
    let res = yield this.api.get('/orders/1');
    expect(res.status).to.equal(200);

    expect(res.response.shippingAddress.id).to.equal(addressId + 1);
  });

});