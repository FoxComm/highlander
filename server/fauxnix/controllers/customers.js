'use strict';

const
  parse = require('co-body'),
  _     = require('lodash');

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
    .get('/customers/:customer/addresses/:address', function *() {
      this.body = this.address;
      this.status = 200;
    })
    .post('/customers/:customer/addresses', function *() {
      let
        body = yield parse.json(this),
        address = new Address(body);
      address.customerId = this.customer.id;
      this.status = 201;
      this.body = address;
    })
    .post('/customers/:customer/addresses/:address/default', function *() {
      let defaultAddress = _.find(Address.findByCustomer(this.customer.id), function(item) {
        return item.isDefault === true;
      });
      defaultAddress.amend({isDefault: false});

      this.address.amend({isDefault: true});
      this.status = 200;
    })
    .delete('/customers/:customer/addresses/default', function *() {
      let addresses = Address.findByCustomer(this.customer.id).filter(function(item) {
        return item.isDefault === true;
      });
      if (addresses.length > 0) {
        addresses[0].amend({isDefault: false});
      }
      this.status = 204;
    });
};
