'use strict';

const parse = require('co-body');

module.exports = function(app, router) {
  const
    Order = app.seeds.models.Order,
    Note  = app.seeds.models.Note;

  router
    .param('order', function *(id, next) {
      this.order = Order.generate(id);
      yield next;
    })
    .get('/orders/:order', function *() {
      this.body = this.order.toJSON();
    })
    .get('/orders/:order/viewers', function *() {
      this.body = this.order.viewers();
    })
    .get('/orders/:order/notes', function *() {
      this.body = this.order.notes();
    })
    .post('/orders/:order/notes', function *() {
      let
        body = yield parse.json(this),
        note = new Note(body);
      this.status = 201;
      this.body = note.toJSON();
    })
    .get('/orders', function *() {
      this.body = Order.generateList();
    })
    .post('/orders', function *() {
      let
        body = yield parse.json(this),
        order = new Order(body);
      this.status = 201;
      this.body = order.toJSON();
    });
};
