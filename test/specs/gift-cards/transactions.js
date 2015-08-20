'use strict';

describe('GiftCard Transactions #GET', function () {
  it('should get a list of transactions', function *() {
    let
      res   = yield this.api.get('/gift-cards/1/transactions'),
      transactions = res.response;
    expect(res.status).to.equal(200);
    expect(transactions).to.have.length(7);
  });
});
