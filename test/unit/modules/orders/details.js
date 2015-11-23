import _ from 'lodash';

const { reducer, ...actions } = importModule('orders/details.js', [
  'collectLineItems',
  'orderLineItemsStartDelete',
  'orderLineItemsCancelDelete'
]);

describe('order details module', function() {

  context('collectLineItems', function() {

    const orderLineItems = require('../../../fixtures/order-line-items.json');

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

});
