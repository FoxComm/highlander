'use strict';

const parse = require('co-body');

module.exports = function(app, router) {
  const Customer = app.seeds.models.Customer;

  router
    .param('customer', function *(id, next) {
      this.customer = Customer.generate(id);
      yield next;
    })
    .get('/customers/:customer', function *() {
      this.body = this.customer.toJSON();
    })
    .get('/customers', function *() {
      let customers = Customer.generateList();
      this.body = customers;
    })
    .post('/customers', function *() {
      let
        body = yield parse.json(this),
        customer = new Customer(body);
      this.status = 201;
      this.body = {customer: customer.toJSON()};
    });
};
