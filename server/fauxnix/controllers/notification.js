'use strict';

//const parse = require('co-body');

module.exports = function(app, router) {
  const Order = app.seeds.models.Order;
  const Notification = app.seeds.models.Notification;

  router
    .param('order', function *(id, next) {
      this.order = Order.generate();
      yield next;
    })
    .get('/order/:order/notifications', function *() {
      this.body = Notification.generateList();
    });
};
