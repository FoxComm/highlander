import _ from 'lodash';
import nock from 'nock';

const { default: reducer, ...actions } = importSource('modules/rmas/list.js', [
  'actionFetch',
  'actionReceived',
  'actionSetFetchParams'
]);

describe('Rmas module', function() {
  const rmaEntity = {
    entityType: 'rma'
  };

  const orderEntity = {
    entityType: 'order',
    entityId: 'ABC-123'
  };

  function rmaUri(entity) {
    if (entity.entityId) {
      return `/api/v1/rmas/${entity.entityType}/${entity.entityId}`;
    } else {
      return '/api/v1/rmas';
    }
  }

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
        .get(rmaUri(rmaEntity))
        .reply(200, rmaPayload);

      nock(phoenixUrl)
        .get(rmaUri(orderEntity))
        .reply(200, rmaPayload);
    });

    after(function() {
      nock.cleanAll();
    });

    it('fetchRmas', function*() {
      const expectedActions = [
        actions.actionFetch,
        actions.actionSetFetchParams,
        { type: actions.actionReceived, payload: [rmaEntity, rmaPayload]}
      ];

      yield expect(actions.fetchRmas(), 'to dispatch actions', expectedActions);
    });

    it('fetchRmas for child', function*() {
      const expectedActions = [
        actions.actionFetch,
        actions.actionSetFetchParams,
        { type: actions.actionReceived, payload: [orderEntity, rmaPayload]}
      ];

      yield expect(actions.fetchRmas(orderEntity), 'to dispatch actions', expectedActions);
    });
  });
});
