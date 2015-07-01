'use strict';

describe('Customers #GET', function() {

  it('should find a customer', function *() {
    let
      res       = yield this.api.get('/customers/1'),
      customer  = res.response;
    expect(res.status).to.equal(200);
    expect(customer.id).to.equal(1);
    expect(customer.createdAt).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
    expect(customer.firstName).to.be.a('string');
    expect(customer.lastName).to.be.a('string');
    expect(customer.email).to.match(/^[a-z0-9_.%+\-]+@[0-9a-z.\-]+\.[a-z]{2,6}$/i);
    expect(customer.role).to.match(/^(admin|customer|vendor)/);
    expect(customer.blocked).to.be.a('boolean');
    expect(customer.cause).to.be.a('string');
    expect(customer.modality).to.be.a('string');
  });

  it('should not find a customer', function *() {
    try {
      yield this.api.get('/customers/9999999');
    } catch(err) {
      expect(err.status).to.equal(404);
      expect(err.response.error).to.equal('Cannot find Customer');
    }
  });

  it('should get an array of customers', function *() {
    let
      res       = yield this.api.get('/customers'),
      customers = res.response;
    expect(res.status).to.equal(200);
    expect(customers).to.have.length(50);
  });

  it('should get 20 customers', function *() {
    let
      res       = yield this.api.get('/orders', {limit: 20}),
      customers = res.response;
    expect(res.status).to.equal(200);
    expect(customers).to.have.length(20);
  });

  it('should get a new page of customers', function *() {
    let
      res       = yield this.api.get('/orders', {page: 2}),
      customers = res.response;
    expect(res.status).to.equal(200);
    expect(customers).to.have.length(50);
  });

  it('should get a new page of 10 customers', function *() {
    let
      res       = yield this.api.get('/customers', {limit: 10, page: 2}),
      customers = res.response;
    expect(res.status).to.equal(200);
    expect(customers).to.have.length(10);
  });
});
