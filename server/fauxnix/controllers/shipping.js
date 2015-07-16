'use strict';

const parse = require('co-body');

module.exports = function(app, router) {
  const ShippingMethod = app.seeds.models.ShippingMethod;

  router.get('/shipping-methods', function *() {
    let
      limit = 4,
      page  = 1;
    this.body = ShippingMethod.paginate(limit, page);
  });
};
