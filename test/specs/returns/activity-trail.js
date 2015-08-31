'use strict';

describe('Return Activities #GET', function() {

  it('should get a list of activities', function *() {
    let
      res         = yield this.api.get('/returns/1/activity-trail'),
      activities  = res.response;
    expect(res.status).to.equal(200);
    expect(activities).to.be.instanceof(Array);
  });
});
