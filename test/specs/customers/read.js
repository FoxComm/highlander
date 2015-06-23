'use strict';

describe('Customers #GET', function() {

  it('should find a customer', function *() {
    let
      res       = yield this.api.get('/customers/1'),
      customer  = res.response;
    expect(res.status).to.equal(200);
    expect(customer.id).to.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    expect(customer.firstName).to.be.a('string');
    expect(customer.lastName).to.be.a('string');
    expect(customer.email).to.match(/^[a-z0-9_.%+\-]+@[0-9a-z.\-]+\.[a-z]{2,6}$/i);
    expect(customer.role).to.match(/^(admin|customer|vendor)/);
    expect(customer.blocked).to.be.a('boolean');
    expect(customer.cause).to.be.a('string');
    expect(customer.createdAt).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
  });

  it('should get an array of customers', function *() {
    let
      res       = yield this.api.get('/customers'),
      customers = res.response;
    expect(res.status).to.equal(200);
    expect(customers).to.have.length(50);
  });
});
