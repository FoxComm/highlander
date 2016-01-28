import _ from 'lodash';
import nock from 'nock';

const { 
  default: reducer,
  collectLineItems,
  updateLineItemCount,
  orderLineItemsFetchSuccess,
  orderLineItemsStartDelete,
  orderLineItemsCancelDelete,
  orderLineItemsStartEdit,
  orderLineItemsCancelEdit,
  orderLineItemsRequest,
  orderLineItemsRequestSuccess,
  deleteLineItem
} = requireSource('modules/orders/line-items.js');

const { orderSuccess } = requireSource('modules/orders/details.js');

describe('order details module line items', function() {

  const orderLineItems = require('../../../fixtures/order-line-items.json');

  context('collectLineItems', function() {

    it('should collapse non-unique skus', function() {
      const result = collectLineItems(orderLineItems);
      expect(result.length).to.be.equal(3);
    });

    it('should sum non-unique sku quantities', function() {
      const result = collectLineItems(orderLineItems);
      const skuYax = _.find(result, obj => obj.sku === 'SKU-YAX');
      expect(skuYax.quantity).to.be.equal(3);
    });

    it('should save quantity for unique skus', function() {
      const result = collectLineItems(orderLineItems);
      const skuZya = _.find(result, obj => obj.sku === 'SKU-ZYA');
      expect(skuZya.quantity).to.be.equal(1);
    });

  });

  context('deleting line item', function() {

    it('orderLineItemsStartDelete should set proper state', function() {
        const initialState = {
            isFetching: false,
            currentOrder: {},
            currentSkus: [],
            isEditing: false,
            isUpdating: false,
            isDeleting: false,
            skuToUpdate: null,
            skuToDelete: null,
            items: []
        };

      const sku = 'SKU-ABC';
      const newState = reducer(initialState, orderLineItemsStartDelete(sku));
      expect(newState.skuToDelete).to.be.equal(sku);
      expect(newState.isDeleting).to.be.equal(true);
    });

    it('orderLineItemsCancelDelete should set proper state', function() {
      const sku = 'SKU-ABC';
      const initialState = {
          isFetching: false,
          currentSkus: [],
          isEditing: true,
          isUpdating: false,
          isDeleting: false,
          skuToUpdate: null,
          skuToDelete: sku,
          items: []
      };

      const newState = reducer(initialState, orderLineItemsCancelDelete(sku));
      expect(newState.skuToDelete).to.be.equal(null);
      expect(newState.isDeleting).to.be.equal(false);
    });

  });

  context('updating line item', function() {

    it('orderLineItemsStartEdit should set proper state', function() {
      const initialState = {
        isFetching: false,
        currentSkus: [],
        isEditing: false,
        isUpdating: false,
        isDeleting: false,
        skuToUpdate: null,
        skuToDelete: null,
        items: []
      };

      const newState = reducer(initialState, orderLineItemsStartEdit());
      expect(newState.isEditing).to.be.equal(true);
    });

    it('orderLineItemsCancelEdit should set proper state', function() {
      const sku = 'SKU-ABC';
      const initialState = {
          isFetching: false,
          currentSkus: [],
          isEditing: true,
          isUpdating: false,
          isDeleting: false,
          skuToUpdate: null,
          skuToDelete: sku,
          items: []
      };

      const newState = reducer(initialState, orderLineItemsCancelEdit());
      expect(newState.isEditing).to.be.equal(false);
    });

    it('orderLineItemsCancelEdit should copy line items from order', function() {
      const sku = 'SKU-ABC';
      const initialState = {
        isFetching: false,
        currentSkus: orderLineItems,
        isEditing: true,
        isUpdating: false,
        isDeleting: false,
        skuToUpdate: null,
        skuToDelete: sku,
        items: []
      };

      const newState = reducer(initialState, orderLineItemsCancelEdit());
      expect(newState.items).to.deep.equal(collectLineItems(orderLineItems));
    });

  });

  context('async actions', function() {

    const orderUrl = refNum => `/api/v1/orders/${refNum}`;
    const orderLineItemsUrl = refNum => orderUrl(refNum) + '/line-items';
    const orderRemorsePeriodUrl = refNum => orderUrl(refNum) + '/increase-remorse-period';
    const orderRef = 'ABCD1234-11';
    const sku = 'SKU-ABC';
    const orderPayload = require('../../../fixtures/order.json');

    context('deleting line items', function() {

      beforeEach(function() {
        nock(phoenixUrl)
          .get(orderUrl(orderRef))
          .reply(200, orderPayload)
          .post(orderLineItemsUrl(orderRef))
          .reply(200, orderPayload);
      });

      afterEach(function() {
        nock.cleanAll();
      });

      it('should trigger delete confirmation firstly', function*() {
        const expectedActions = [
          orderLineItemsStartDelete
        ];

        yield expect(updateLineItemCount(orderRef, sku, 0), 'to dispatch actions', expectedActions);
      });

      it('should trigger delete API calls and proper actions', function*() {
        const expectedActions = [
          orderLineItemsRequest,
          orderSuccess,
          orderLineItemsRequestSuccess,
          orderLineItemsFetchSuccess
        ];

        yield expect(updateLineItemCount({referenceNumber: orderRef}, sku, 0, false),
                     'to dispatch actions', expectedActions);
      });

      it('deleteLineItem should trigger line item delete actions', function*() {
        const expectedActions = [
          orderLineItemsRequest,
          orderSuccess,
          orderLineItemsRequestSuccess,
          orderLineItemsFetchSuccess
        ];

        yield expect(deleteLineItem({referenceNumber: orderRef}, sku),
                     'to dispatch actions', expectedActions);
      });

    });

  });

});
