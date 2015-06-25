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
    .param('notification', function *(id, next) {
      this.notification = Notification.generate(id);
      yield next;
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
    .get('/orders/:order', function *() {
      this.body = this.order.toJSON();
    })
    .patch('/orders/:order', function *() {
      let
        body = yield parse.json(this);
      this.order.update(body);
      this.status = 200;
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
    .get('/orders/:order/activity-trail', function *() {
      this.body = this.order.activityTrail();
    })
    .get('/orders/:order/notifications', function *() {
      this.body = this.order.notifications();
    })
    .post('/orders/:order/notifications/:notification', function *() {
      this.body = this.notification.toJSON();
    });
};
