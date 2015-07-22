'use strict';

describe('GiftCards #Get', function() {
  it('should get an array of gift cards', function *() {
    let
      res   = yield this.api.get('/gift-cards', {limit: 20}),
      cards = res.response;
    expect(res.status).to.equal(200);
    expect(cards).to.have.length(20);
  });

  it('should get a new page of gift cards', function *(){
    let
      res   = yield this.api.get('/gift-cards', {page: 2}),
      cards = res.response;
    expect(res.status).to.equal(200);
    expect(cards).to.have.length(50);
  });

  it('should get a new page of 10 gift cards', function *(){
    let
      res  = yield this.api.get('/gift-cards', {limit: 10, page:2}),
      cards = res.response;
    expect(res.status).to.equal(200);
    expect(cards).to.have.length(10);
  });
});
