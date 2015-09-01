'use strict';

describe('Return Activities', function() {

  context('#GET', function() {
    it('should get a list of activities', function *() {
      let res = yield this.api.get('/returns/1/activity-trail');
      let activities = res.response;

      expect(res.status).to.equal(200);
      expect(activities).to.be.instanceof(Array);
    });
  });

});
