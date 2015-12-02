import _ from 'lodash';
import nock from 'nock';

const { default: reducer, ...actions } = importSource('modules/skus.js', [
  'requestSkus',
  'receiveSkus',
  'failSkus'
]);

describe('skus module', function() {

  context('reducers', function() {

    it('requestSkus should return proper state', function() {
      const initialState = {
        isFetching: false,
        skus: []
      };
      const newState = reducer(initialState, actions.requestSkus());
      expect(newState.isFetching).to.be.equal(true);
    });

    it('receiveSkus should return proper state', function() {
      const initialState = {
        isFetching: true,
        skus: []
      };
      const skus = [];
      const payload = {};

      const newState = reducer(initialState, actions.receiveSkus(payload));
      expect(newState.isFetching).to.be.equal(false);
      expect(newState.skus).to.deep.equal(payload);
    });

    it('failSkus should return proper state', function() {
      const initialState = {
        isFetching: true,
        skus: []
      };
      const newState = reducer(initialState, actions.failSkus('error'));
      expect(newState.isFetching).to.be.equal(false);
    });

  });

});
