'use strict';

describe('Return Notes', function() {

  context('#GET', function() {
    it('should get a list of notes', function *() {
      let res = yield this.api.get('/returns/1/notes');
      let notes = res.response;

      expect(res.status).to.equal(200);
      expect(notes).to.be.instanceof(Array);
    });
  });

  context('#POST', function() {
    it('should create a note', function *() {
      let data = {body: 'this is a note'};
      let res = yield this.api.post('/returns/1/notes', data);
      let note = res.response;

      expect(res.status).to.equal(201);
      expect(note.id).to.be.a('number');
      expect(note.createdAt).to.match(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/);
      expect(note.body).to.equal('this is a note');
      expect(note.author).to.be.an('object');
    });
  });

});
