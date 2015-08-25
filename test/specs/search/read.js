'use strict';

describe('Search #GET', function() {
  context('when searching orders', function() {
    it('should get a list of orders', function *() {
      let data = {
        q: 'orderStatus:cart'
      };

      let res = yield this.api.get('/search/orders', data);
      expect(res.status).to.equal(200);
      expect(res.response).to.have.length(20);
    });

    it('should get a list of 5 orders', function *() {
      let data = {
        q: 'paymentStatus:capture',
        size: 5
      };

      let res = yield this.api.get('/search/orders', data);
      expect(res.status).to.equal(200);
      expect(res.response).to.have.length(5);
    });
  });

  context('when searching customers', function() {
    it('should get a list of customers', function *() {
      let data = {
        q: 'email:gov'
      };

      let res = yield this.api.get('/search/customers', data);
      expect(res.status).to.equal(200);
      expect(res.response).to.have.length(20);
    });

    it('should get a list of 5 customers', function *() {
      let data = {
        q: 'email:edu',
        size: 5
      };

      let res = yield this.api.get('/search/customers', data);
      expect(res.status).to.equal(200);
      expect(res.response).to.have.length(5);
    });
  });
});
