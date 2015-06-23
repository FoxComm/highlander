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
    expect(note.id).to.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    expect(note.createdAt).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
    expect(note.body).to.equal('this is a note');
    expect(note.author).to.be.an('object');
  });
});
