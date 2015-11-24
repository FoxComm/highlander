import _ from 'lodash';
import nock from 'nock';

const { reducer, ...actions } = importModule('products.js', [
  'requestProducts',
  'receiveProducts',
  'failProducts'
]);

describe('products module', function() {

  context('reducers', function() {

    it('requestProducts should return proper state', function() {
      const initialState = {
        isFetching: false,
        products: []
      };
      const newState = reducer(initialState, actions.requestProducts());
      expect(newState.isFetching).to.be.equal(true);
    });

    it('receiveProducts should return proper state', function() {
      const initialState = {
        isFetching: true,
        products: []
      };
      const payload = [];

      const newState = reducer(initialState, actions.receiveProducts(payload));
      expect(newState.isFetching).to.be.equal(false);
      expect(newState.products).to.deep.equal(payload);
    });

    it('failProducts should return proper state', function() {
      const initialState = {
        isFetching: true,
        products: []
      };
      const newState = reducer(initialState, actions.failProducts('error'));
      expect(newState.isFetching).to.be.equal(false);
    });

  });

});
