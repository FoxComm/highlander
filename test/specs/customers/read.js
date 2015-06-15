'use strict';

describe('Customers #GET', function() {

  it('should get', function *() {
    let res = yield this.Api.get('/');
    console.log(res[1]);
    expect(res[0].statusCode).to.equal(200);
  });

  it('should get an array of customers', function *() {
    let res = yield this.Api.get('/api/v1/customers');
    console.log(res[1]);
    expect(res[0].statusCode).to.equal(200);
  });

  it('should find a customer', function *() {
    let res = yield this.Api.get('/api/v1/customers/1');
    console.log(res[1]);
    expect(res[0].statusCode).to.equal(200);
    expect(res[1].firstName).to.be.a.string;
  });
});
