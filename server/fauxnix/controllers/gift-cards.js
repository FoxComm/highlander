'use strict';

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
    });
};
