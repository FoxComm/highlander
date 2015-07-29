'use strict';

const
  parse  = require('co-body'),
  moment = require('moment'),
  _      = require('underscore'),
  Chance = require('chance');

const
  chance = new Chance();

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
        cards = [],
        balance = body.balance,
        data = {
          type: body.cardType,
          subType: body.subType,
          balance: balance,
          currentBalance: balance,
          availableBalance: balance,
          state: 'Active',
          date: moment.utc().add(1, 'y').toDate()
        },
        quantity = body.quantity;

      if (body['customers[]']) quantity = body['customers[]'].length;

      for (let i = 0; i < quantity; i++) {
        let
          cardData = _.extend({}, data);
        if (body.sendToCustomer) {
          cardData.customerId = +body['customers[]'][i];
        }
        cardData.cardNumber = chance.integer({min: 1000000000000000, max: 9999999999999999});
        cards.push(new GiftCard(cardData));
      }

      this.body = cards;
    })
    .get('/gift-cards/:giftcard', function *() {
      this.body = this.card;
    });
};
