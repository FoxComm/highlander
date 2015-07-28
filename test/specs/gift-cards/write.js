'use strict';

describe('GiftCards #POST', function() {
  it('should return a gift card', function*() {
    let
      data = {
        cardType: 'Appeasement',
        subType: 'Broken',
        balance: 50,
        quantity: 1
      },
      res = yield this.api.post('/gift-cards', data),
      cards = res.response;

    expect(res.status).to.equal(200);
    expect(cards).to.have.length(1);
  });

  it('should create an array of gift cards', function *() {
    let
      data = {
        cardType: 'Appeasement',
        balance: 50,
        'customers[]': [1, 2, 3],
        quantity: 3
      },
      res   = yield this.api.post('/gift-cards', data),
      cards = res.response;

    expect(res.status).to.equal(200);
    expect(cards).to.have.length(3);
  });
});
