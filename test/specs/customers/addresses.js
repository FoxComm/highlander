'use strict';

describe('Customer Addresses #GET', function() {
  it('should get an array of addresses for customer', function *() {
    let
      res             = yield this.api.get('/customers/1/addresses'),
      addresses       = res.response,
      defaultAddress  = addresses.filter(function(a) { return a.isDefault; }),
      activeAdress    = addresses.filter(function(a) { return a.isActive; });
    expect(res.status).to.equal(200);
    expect(addresses).to.have.length(7);
    expect(defaultAddress).to.have.length(1);
    expect(activeAdress).to.have.length(1);
  });
});

describe('Customer Addresses #PATCH', function() {
  it('should update an address for customer', function *() {
    let
      res     = yield this.api.patch('/customers/1/addresses/1', {isActive: true}),
      address = res.response;
    expect(res.status).to.equal(200);
    expect(address.id).to.equal(1);
    expect(address.isActive).to.equal(true);
  });
});

describe('Customer Addresses #POST', function() {
  it('should create an address for customer', function *() {
    let
      street1 = '123 Awesome St',
      res     = yield this.api.post('/customers/1/addresses', {street1: street1, isActive: true}),
      address = res.response;
    expect(res.status).to.equal(201);
    expect(address.street1).to.equal(street1);
    expect(address.isActive).to.equal(true);
  });
});

describe('Customer Addresses default #POST', function () {
  it('should set an address to default', function *() {
    let
      res     = yield this.api.post('/customers/1/addresses/2/default', {isDefault: true});
    expect(res.status).to.equal(200);
    let
      res2    = yield this.api.get('/customers/1/addresses/2'),
    address2 = res2.response;
    expect(res2.status).to.equal(200);
    expect(address2.isDefault).to.equal(true);
  });
});
