'use strict';

describe('Customers #GET', function() {

  it('should find a customer', function *() {
    let
      res       = yield this.Api.get('/api/v1/customers/1'),
      status    = res[0].statusCode,
      customer  = res[1];
    expect(status).to.equal(200);
    expect(customer.firstName).to.be.a.string;
    expect(customer.lastName).to.be.a.string;
    expect(customer.email).to.match(/^[a-z0-9_.%+\-]+@[0-9a-z.\-]+\.[a-z]{2,6}$/i);
    expect(customer.role).to.match(/^(admin|customer|vendor)/);
    expect(customer.blocked).to.be.a.boolean;
    expect(customer.cause).to.be.a.string;
    expect(customer.createdAt).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
  });

  it('should get an array of customers', function *() {
    let
      res       = yield this.Api.get('/api/v1/customers'),
      status    = res[0].statusCode,
      customers = res[1];
    expect(status).to.equal(200);
    expect(customers).to.have.length(50);
  });
});
