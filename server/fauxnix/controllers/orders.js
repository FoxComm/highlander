'use strict';

const
  _     = require('underscore'),
  parse = require('co-body');

module.exports = function(app, router) {
  const
    Order = app.seeds.models.Order,
    Note  = app.seeds.models.Note,
    Notification = app.seeds.models.Notification;

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
      let
        notes = this.order.notes(),
        note  = _(notes).sample();
      if (note) note.isEditable = true;
      this.body = notes;
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
    })
    .param('notification', function *(id, next) {
      this.notification = Notification.generate(id);
      yield next;
    })
    .get('/orders/:order/notifications', function *() {
      this.body = Notification.generateList();
    })
    .post('/orders/:order/notifications/:notification', function *() {
      this.body = this.notification.toJSON();
    });
};
