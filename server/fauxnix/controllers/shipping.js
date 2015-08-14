'use strict';


module.exports = function(app, router) {
  const ShippingMethod = app.seeds.models.ShippingMethod;

  router.get('/shipping-methods', function *() {
    this.body = ShippingMethod.paginate(4, 1);
  });
};
