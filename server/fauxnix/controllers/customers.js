'use strict';

const parse = require('co-body');

module.exports = function(app, router) {
  const Customer = app.seeds.models.Customer;
  const Address = app.seeds.models.Address;

  router
    .param('customer', function *(id, next) {
      this.customer = Customer.generate(id);
      yield next;
    })
    .get('/customers/:customer', function *() {
      this.body = this.customer.toJSON();
    })
    .get('/customers', function *() {
      this.body = Customer.generateList();
    })
    .post('/customers', function *() {
      let
        body = yield parse.json(this),
        customer = new Customer(body);
      this.status = 201;
      this.body = customer.toJSON();
    })
    .param('address', function *(id, next) {
      this.address = Address.generate(id);
      yield next;
    })
    .patch('/customers/:customer/addresses/:address', function *() {
      let
        body = yield parse.json(this);
      this.address.update(body);
      this.status = 200;
      this.body = this.address.toJSON();
    })
    .get('/customers/:customer/addresses', function *() {
      this.body = Address.generateList(7);
    })
    .post('/customers/:customer/addresses', function *() {
      let
        body = yield parse.json(this),
        address = new Address(body);
      this.status = 201;
      this.body = address.toJSON();
    });
};
