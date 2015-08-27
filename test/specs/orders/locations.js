'use strict';

describe('Locations #GET', function() {

  it('should get a list of countries', function *() {
    let
      res     = yield this.api.get('/countries'),
      countries = res.response;
    expect(res.status).to.equal(200);
    expect(countries).to.be.instanceof(Array);
    expect(countries).to.have.length.above(0);
  });

  it('should get a list of country regions', function *() {
    let
      res     = yield this.api.get('/countries/us'),
      regions = res.response;
    expect(res.status).to.equal(200);
    expect(regions).to.be.instanceof(Array);
    expect(regions).to.have.length.above(0);
  });
});
