'use strict';

const
  parse = require('co-body'),
  Chance = require('chance');

const
  chance = new Chance();

module.exports = function(app, router) {
  const
    Order = app.seeds.models.Order,
    Note  = app.seeds.models.Note,
    Notification = app.seeds.models.Notification;

  router
    .param('order', function *(id, next) {
      this.order = Order.findOne(id);
      yield next;
    })
    .param('notification', function *(id, next) {
      this.notification = Notification.generate(id);
      yield next;
    })
    .get('/orders', function *() {
      let query = this.request.query;
      this.body = Order.paginate(query.limit, query.page);
    })
    .post('/orders', function *() {
      let
        body = yield parse.json(this),
        order = new Order(body);
      this.status = 201;
      this.body = order;
    })
    .get('/orders/:order', function *() {
      this.body = this.order;
    })
    .patch('/orders/:order', function *() {
      let
        body = yield parse.json(this);
      this.order.update(body);
      this.status = 200;
      this.body = this.order;
    })
    .post('/orders/:order/edit', function *() {
      this.status = chance.weighted([202, 423], [50, 1]);
      if (this.status === 423) return this.status;
      this.body = this.order;
    })
    .get('/orders/:order/viewers', function *() {
      this.body = this.order.viewers();
    })
    .get('/orders/:order/notes', function *() {
      this.body = Note.findByOrder(this.order.id);
    })
    .post('/orders/:order/notes', function *() {
      let
        body = yield parse.json(this),
        note = new Note(body);
      note.orderId = this.order.id;
      // @todo no notion of auth right now, hard coded to 1 - Tivs
      note.customerId = 1;
      this.status = 201;
      this.body = note;
    })
    .get('/orders/:order/activity-trail', function *() {
      this.body = this.order.activityTrail();
    })
    .get('/orders/:order/notifications', function *() {
      this.body = this.order.notifications();
    })
    .post('/orders/:order/notifications/:notification', function *() {
      this.body = this.notification;
    });
};
