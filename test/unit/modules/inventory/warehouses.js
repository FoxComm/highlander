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
  const sku = 'AC-DC';
  const warehouseId = 1;
  const summaryUrl = sku => `/api/v1/inventory/skus/${sku}/summary`;
  const detailsUrl = (sku, warehouseId) => `/api/v1/inventory/skus/${sku}/${warehouseId}`;

  context('fetchSummary', function() {

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

  context('warehousesFetchSummarySuccess', function() {
    const initialState = {};

    it('should return properly formated data', function*() {
      const newState = reducer(initialState, actions.warehousesFetchSummarySuccess(sku, summaryPayload));

      const rows = _.get(newState, [sku, 'summary', 'results', 'rows'], []);
      const total = _.get(newState, [sku, 'summary', 'results', 'total'], -1);
      const from = _.get(newState, [sku, 'summary', 'results', 'from'], -1);
      const size = _.get(newState, [sku, 'summary', 'results', 'size'], -1);
      expect(rows.length).to.be.equal(1);
      expect(total).to.be.equal(1);
      expect(from).to.be.equal(0);
      expect(size).to.be.equal(25);
    });

  });

  context('warehousesFetchDetailsSuccess', function() {
    const initialState = {};

    it('should return properly formated data', function*() {
      const newState = reducer(initialState, actions.warehousesFetchDetailsSuccess(sku, warehouseId, detailsPaylaod));

      const rows = _.get(newState, [sku, warehouseId, 'results', 'rows'], []);
      const total = _.get(newState, [sku, warehouseId, 'results', 'total'], -1);
      const from = _.get(newState, [sku, warehouseId, 'results', 'from'], -1);
      const size = _.get(newState, [sku, warehouseId, 'results', 'size'], -1);
      expect(rows.length).to.be.equal(4);
      expect(total).to.be.equal(4);
      expect(from).to.be.equal(0);
      expect(size).to.be.equal(25);
    });

  });
});
