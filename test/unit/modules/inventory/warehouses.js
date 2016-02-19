import _ from 'lodash';
import nock from 'nock';

const { default: reducer, ...actions } = importSource('modules/inventory/warehouses.js', [
  'warehousesFetchSummaryStart',
  'warehousesFetchSummarySuccess',
  'warehousesFetchFailed',
  'warehousesFetchDetailsStart',
  'warehousesFetchDetailsSuccess',
  'fetchSummary',
  'fetchDetails',
]);

const summaryPayload = [
  {
    'warehouse': {
      'id': 1,
      'name': 'default'
    },
    'counts': {
      'onHand': 516,
      'onHold': 5,
      'reserved': 75,
      'safetyStock': 7,
      'afs': 429,
      'afsCost': 1415700
    }
  }
];

const detailsPaylaod = [
  {
    'skuType': 'backorder',
    'counts': {
      'onHand': 240,
      'onHold': 3,
      'reserved': 1,
      'afs': 236,
      'afsCost': 778800
    }
  },
  {
    'skuType': 'nonSellable',
    'counts': {
      'onHand': 318,
      'onHold': 27,
      'reserved': 2,
      'afs': 289,
      'afsCost': 953700
    }
  },
  {
    'skuType': 'preorder',
    'counts': {
      'onHand': 256,
      'onHold': 34,
      'reserved': 41,
      'afs': 181,
      'afsCost': 597300
    }
  },
  {
    'skuType': 'sellable',
    'counts': {
      'onHand': 516,
      'onHold': 5,
      'reserved': 75,
      'safetyStock': 7,
      'afs': 429,
      'afsCost': 1415700
    }
  }
];

describe('warehouses module', function() {
  const summaryUrl = sku => `/api/v1/inventory/skus/${sku}/summary`;
  const detailsUrl = (sku, warehouseId) => `/api/v1/inventory/skus/${sku}/${warehouseId}`;

  context('fetchSummary', function() {

    const sku = 'AC-DC';

    before(function() {
      nock(phoenixUrl)
        .get(summaryUrl(sku))
        .reply(200, summaryPayload);
    });

    after(function() {
      nock.cleanAll();
    });

    it('dispatches correct actions', function*() {
      const expectedActions = [
        actions.warehousesFetchSummaryStart,
        actions.warehousesFetchSummarySuccess
      ];

      yield expect(actions.fetchSummary(sku), 'to dispatch actions', expectedActions);
    });

  });

  context('fetchDetails', function() {

    const sku = 'AC-DC';
    const warehouseId = 1;

    before(function() {
      nock(phoenixUrl)
        .get(detailsUrl(sku, warehouseId))
        .reply(200, detailsPaylaod);
    });

    after(function() {
      nock.cleanAll();
    });

    it('dispatches correct actions', function*() {
      const expectedActions = [
        actions.warehousesFetchDetailsStart,
        actions.warehousesFetchDetailsSuccess
      ];

      yield expect(actions.fetchDetails(sku, warehouseId), 'to dispatch actions', expectedActions);
    });

  });
});
