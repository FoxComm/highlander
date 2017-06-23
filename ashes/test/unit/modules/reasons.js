import _ from 'lodash';

import reducer, { reasonsRequested, reasonsReceived, reasonsFailed } from 'modules/reasons';

describe('reasons module', function() {
  const reasonsPayload = require('../../fixtures/reasons.json');

  context('reducers', function() {
    it('reasonsRequested should return proper state', function() {
      const initialState = {
        isFetching: false,
        reasons: [],
      };
      const newState = reducer(initialState, reasonsRequested());
      expect(newState.isFetching).to.be.equal(true);
    });

    it('reasonsReceived should return proper state', function() {
      const initialState = {
        isFetching: true,
        reasons: [],
      };
      const payload = reasonsPayload;
      const reasonType = 'donkeyResons';

      const newState = reducer(initialState, reasonsReceived(payload, reasonType));
      expect(newState.isFetching).to.be.equal(false);
      expect(newState.reasons[reasonType]).to.deep.equal(payload);
    });

    it('reasonsFailed should return proper state', function() {
      const initialState = {
        isFetching: true,
        reasons: [],
      };
      const err = console.error;
      console.error = _.noop;
      const newState = reducer(initialState, reasonsFailed('error'));
      expect(newState.isFetching).to.be.equal(false);
      console.error = err;
    });
  });
});
