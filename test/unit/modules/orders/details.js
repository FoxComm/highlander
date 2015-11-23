import _ from 'lodash';
import nock from 'nock';

const { reducer, ...actions } = importModule('orders/details.js', [
  'collectLineItems',
  'orderLineItemsStartDelete',
  'orderLineItemsCancelDelete',
  'orderLineItemsStartEdit',
  'orderLineItemsCancelEdit',
  'orderRequest',
  'orderSuccess'
]);

describe('order details module', function() {

  const orderLineItems = require('../../../fixtures/order-line-items.json');

  context('collectLineItems', function() {

    it('should collapse non-unique skus', function() {
      const result = actions.collectLineItems(orderLineItems);
      expect(result.length).to.be.equal(3);
    });

    it('should sum non-unique sku quantities', function() {
      const result = actions.collectLineItems(orderLineItems);
      const skuYax = _.find(result, obj => obj.sku === 'SKU-YAX');
      expect(skuYax.quantity).to.be.equal(3);
    });

    it('should save quantity for unique skus', function() {
      const result = actions.collectLineItems(orderLineItems);
      const skuAbc = _.find(result, obj => obj.sku === 'SKU-ABC');
      expect(skuAbc.quantity).to.be.equal(2);
      const skuZya = _.find(result, obj => obj.sku === 'SKU-ZYA');
      expect(skuZya.quantity).to.be.equal(1);
    });

  });

  context('deleting line item', function() {

    it('orderLineItemsStartDelete should set proper state', function() {
      const initialState = {
        isFetching: false,
        currentOrder: {},
        lineItems: {
          isEditing: false,
          isUpdating: false,
          isDeleting: false,
          skuToUpdate: null,
          skuToDelete: null,
          items: []
        }
      };

      const sku = 'SKU-ABC';
      const newState = reducer(initialState, actions.orderLineItemsStartDelete(sku));
      expect(newState.lineItems.skuToDelete).to.be.equal(sku);
      expect(newState.lineItems.isDeleting).to.be.equal(true);
    });

    it('orderLineItemsCancelDelete should set proper state', function() {
      const sku = 'SKU-ABC';
      const initialState = {
        isFetching: false,
        currentOrder: {},
        lineItems: {
          isEditing: true,
          isUpdating: false,
          isDeleting: false,
          skuToUpdate: null,
          skuToDelete: sku,
          items: []
        }
      };

      const newState = reducer(initialState, actions.orderLineItemsCancelDelete(sku));
      expect(newState.lineItems.skuToDelete).to.be.equal(null);
      expect(newState.lineItems.isDeleting).to.be.equal(false);
    });

  });

  context('updating line item', function() {

    it('orderLineItemsStartEdit should set proper state', function() {
      const initialState = {
        isFetching: false,
        currentOrder: {},
        lineItems: {
          isEditing: false,
          isUpdating: false,
          isDeleting: false,
          skuToUpdate: null,
          skuToDelete: null,
          items: []
        }
      };

      const newState = reducer(initialState, actions.orderLineItemsStartEdit());
      expect(newState.lineItems.isEditing).to.be.equal(true);
    });

    it('orderLineItemsCancelEdit should set proper state', function() {
      const sku = 'SKU-ABC';
      const initialState = {
        isFetching: false,
        currentOrder: {},
        lineItems: {
          isEditing: true,
          isUpdating: false,
          isDeleting: false,
          skuToUpdate: null,
          skuToDelete: sku,
          items: []
        }
      };

      const newState = reducer(initialState, actions.orderLineItemsCancelEdit());
      expect(newState.lineItems.isEditing).to.be.equal(false);
    });

    it('orderLineItemsCancelEdit should copy line items from order', function() {
      const sku = 'SKU-ABC';
      const initialState = {
        isFetching: false,
        currentOrder: {
          lineItems: {
            skus: orderLineItems
          }
        },
        lineItems: {
          isEditing: true,
          isUpdating: false,
          isDeleting: false,
          skuToUpdate: null,
          skuToDelete: sku,
          items: []
        }
      };

      const newState = reducer(initialState, actions.orderLineItemsCancelEdit());
      expect(newState.lineItems.items).to.deep.equal(actions.collectLineItems(orderLineItems));
    });

  });

  context('async actions', function() {

    const orderUrl = refNum => `/api/v1/orders/${refNum}`;

    context('fetching orders', function() {
      const orderRef = 'ABC-123';

      before(function() {
        nock(phoenixUrl)
          .get(orderUrl(orderRef))
          .reply(200, {});
      });

      after(function() {
        nock.cleanAll();
      });

      it('dispatches correct actions', function*() {
        const expectedActions = [
          actions.orderRequest,
          actions.orderSuccess
        ];

        yield expect(actions.fetchOrder(orderRef), 'to dispatch actions', expectedActions);
      });
    });

    context('updating orders on server', function() {
      const orderRef = 'ABC-123';

      before(function() {
        nock(phoenixUrl)
          .get(orderUrl(orderRef))
          .reply(200, {})
          .patch(orderUrl(orderRef))
          .reply(200, {});
      });

      after(function() {
        nock.cleanAll();
      });

      it('dispatches correct actions', function*() {
        const expectedActions = [
          actions.orderRequest,
          actions.orderSuccess
        ];

        yield expect(actions.fetchOrder(orderRef), 'to dispatch actions', expectedActions);
      });
    });

  });

});
