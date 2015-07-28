'use strict';

const
  parse = require('co-body');

module.exports = function(app, router) {
  const GiftCard = app.seeds.models.GiftCard;

  router
    .param('giftcard', function *(id, next) {
      this.card = GiftCard.findOne(id);
      yield next;
    })
    .get('/gift-cards', function *() {
      let query = this.request.query;
      this.body = GiftCard.paginate(query.limit, query.page);
    })
    .post('/gift-cards', function *() {
      let
        body = yield parse.json(this),
        cards = [];

      for (let i = body.quantity; i > 0; i--) {
        cards.push(new GiftCard());
      }

      this.body = cards;
    });
};
