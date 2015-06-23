'use strict';

describe('Order Notes #GET', function() {

  it('should get a list of notes', function *() {
    let
      res     = yield this.api.get('/orders/1/notes'),
      notes   = res.response;
    expect(res.status).to.equal(200);
    expect(notes).to.have.length.of.at.most(4);
  });
});

describe('Order Notes #POST', function() {

  it('should create a note', function *() {
    let
      data    = {body: 'this is a note'},
      res     = yield this.api.post('/orders/1/notes', data),
      note    = res.response;
    expect(res.status).to.equal(201);
  });
});
