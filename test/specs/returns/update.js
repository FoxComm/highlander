'use strict';

describe('Returns #PATCH', function() {
  it('should update a return status', function *() {
    let
      res     = yield this.api.patch('/returns/1', {returnStatus: 'Processing'}),
      ret   = res.response;

    expect(res.status).to.equal(200);
    expect(ret.id).to.equal(1);
    expect(ret.returnStatus).to.equal('Processing');
  });
});
