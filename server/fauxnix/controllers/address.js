'use strict';

const parse = require('co-body');

module.exports = function(app, router) {
  const Address = app.seeds.models.Address;

  router
    .param('address', function *(id, next) {
      this.address = Address.generate(id);
      yield next;
    })
    .get('/addresses/:address', function *() {
      this.body = this.address.toJSON();
    })
    .patch('/addresses/:address', function *() {
      let
        body = yield parse.json(this);
      this.address.update(body);
      this.status = 200;
      this.body = this.address.toJSON();
    })
    .get('/addresses', function *() {
      this.body = Address.generateList(7);
    })
    .post('/addresses', function *() {
      let
        body = yield parse.json(this),
        address = new Address(body);
      this.status = 201;
      this.body = address.toJSON();
    });
};
