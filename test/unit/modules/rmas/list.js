import _ from 'lodash';
import nock from 'nock';

const { reducer, ...actions } = importModule('rmas/list.js', [
  'actionFetch',
  'actionReceived',
  'actionSetFetchParams'
]);

describe('Rmas module', function() {
  const entity = {
    entityType: 'rma'
  };

  const rma = require('../../../fixtures/rma.json');

  const rmaPayload = [
    {
      pagination: {
        from:0,
        size:50,
        pageNo:1,
        total:1
      },
      result: [
        {...rma}
      ]
    }
  ];

  context('async actions', function() {
    before(function() {
      nock(phoenixUrl)
        .get('/api/v1/rmas')
        .reply(200, rmaPayload);
    });

    after(function() {
      nock.cleanAll();
    });

    it('fetchRmas', function*() {
      const expectedActions = [
        actions.actionFetch,
        actions.actionSetFetchParams,
        { type: actions.actionReceived, payload: [entity, rmaPayload]}
      ];

      yield expect(actions.fetchRmas(), 'to dispatch actions', expectedActions);
    });
  });
});
