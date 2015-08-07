'use strict';

const
  parse  = require('co-body'),
  moment = require('moment'),
  _      = require('underscore'),
  Chance = require('chance');

const
  chance = new Chance();

module.exports = function(app, router) {
  const
    GiftCard            = app.seeds.models.GiftCard,
    Note                = app.seeds.models.Note,
    Activity            = app.seeds.models.Activity,
    GiftCardTransaction = app.seeds.models.GiftCardTransaction;

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
        cardData.code = chance.integer({min: 1000000000000000, max: 9999999999999999});
        cards.push(new GiftCard(cardData));
      }

      this.body = cards;
    })
    .patch('/gift-cards/:giftcard', function *() {
      let
        body = yield parse.json(this);

      this.card.amend(body);

      this.body = this.card;
    })
    .get('/gift-cards/:giftcard', function *() {
      this.body = this.card;
    })
    .get('/gift-cards/:giftcard/notes', function *() {
      this.body = Note.findAll('giftCardId', this.card.id);
    })
    .post('/gift-cards/:giftcard/notes', function *() {
      let
        body = yield parse.json(this),
        note = new Note(body);
      note.giftCardId = this.card.id;
      // @todo no notion of auth right now, hard coded to 1 - Tivs
      note.customerId = 1;
      this.status = 201;
      this.body = note;
    })
    .get('/gift-cards/:giftcard/activity-trail', function *() {
      this.body = Activity.findAll('giftCardId', this.card.id);
    })
    .get('/gift-cards/:giftcard/transactions', function *() {
      this.body = GiftCardTransaction.findAll('giftCardId', this.card.id);
    });
};
