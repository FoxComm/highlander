'use strict';

const parse = require('co-body');

module.exports = function(app, router) {
  const Customer = app.seeds.models.Customer;
  const Address = app.seeds.models.Address;

  router
    .param('customer', function *(id, next) {
      this.customer = Customer.findOne(id);
      yield next;
    })
    .param('address', function *(id, next) {
      this.address = Address.findOne(id);
      yield next;
    })
    .get('/customers/:customer', function *() {
      this.body = this.customer;
    })
    .get('/customers', function *() {
      let query = this.request.query;
      this.body = Customer.paginate(query.limit, query.page);
    })
    .post('/customers', function *() {
      let
        body = yield parse.json(this),
        customer = new Customer(body);
      this.status = 201;
      this.body = customer;
    })
    .patch('/customers/:customer/addresses/:address', function *() {
      let body = yield parse.json(this);
      this.address.amend(body);
      this.status = 200;
      this.body = this.address;
    })
    .get('/customers/:customer/addresses', function *() {
      this.body = Address.findByCustomer(this.customer.id);
    })
    .post('/customers/:customer/addresses', function *() {
      let
        body = yield parse.json(this),
        address = new Address(body);
      address.customerId = this.customer.id;
      this.status = 201;
      this.body = address;
    });
};
