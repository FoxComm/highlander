'use strict';

module.exports = function(app, router) {
  const Sku = app.seeds.models.Sku;

  router
    .param('product', function *(id, next) {
      this.product = Sku.generate(id);
      yield next;
    })
    .get('/products', function *() {
      this.body = Sku.generateList(10);
    });
};
