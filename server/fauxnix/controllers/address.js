'use strict';

//const parse = require('co-body');

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
    .get('/addresses', function *() {
      this.body = Address.generateList(10);
    });
};
