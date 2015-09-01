'use strict';

describe('Return Viewers', function() {

  context('#GET', function() {
    it('should get a list of viewers', function *() {
      let res = yield this.api.get('/returns/1/viewers');
      let viewers = res.response;

      expect(res.status).to.equal(200);
      expect(viewers).to.be.instanceof(Array);
    });
  });

});
