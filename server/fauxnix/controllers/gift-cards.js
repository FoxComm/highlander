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
        balance = body.balance * 100,
        data = {
          type: body.cardType,
          subType: body.subType,
          balance: balance,
          currentBalance: balance,
          availableBalance: balance,
          state: 'Active',
          date: moment.utc().add(1, 'y').toDate()
        };

      for (let i = 0; i < body.quantity; i++) {
        let
          cardData = _.extend({}, data);
        if (body.sendToCustomer) {
          cardData.customerId = +body['customers[]'][i];
        }
        cardData.cardNumber = chance.integer({min: 1000000000000000, max: 9999999999999999});
        cards.push(new GiftCard(cardData));
      }

      this.body = cards;
    });
};
