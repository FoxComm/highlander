import _ from 'lodash';
import nock from 'nock';

const { default: reducer, ...actions } = importSource('modules/orders/details.js', [
  'orderRequest',
  'orderSuccess',
  'increaseRemorsePeriod'
]);

describe('order details module', function() {

  context('async actions', function() {

    const orderUrl = refNum => `/api/v1/orders/${refNum}`;
    const orderLineItemsUrl = refNum => orderUrl(refNum) + '/line-items';
    const orderRemorsePeriodUrl = refNum => orderUrl(refNum) + '/increase-remorse-period';
    const orderRef = 'ABCD1234-11';
    const sku = 'SKU-ABC';
    const orderPayload = require('../../../fixtures/order.json');

    context('fetching orders', function() {

      before(function() {
        nock(phoenixUrl)
          .get(orderUrl(orderRef))
          .reply(200, orderPayload);
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

      before(function() {
        nock(phoenixUrl)
          .get(orderUrl(orderRef))
          .reply(200, orderPayload)
          .patch(orderUrl(orderRef))
          .reply(200, orderPayload);
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

    context('remorse period', function() {

      beforeEach(function() {
        nock(phoenixUrl)
          .get(orderUrl(orderRef))
          .reply(200, orderPayload)
          .post(orderRemorsePeriodUrl(orderRef))
          .reply(200, orderPayload);
      });

      afterEach(function() {
        nock.cleanAll();
      });

      it('increaseRemorsePeriod should trigger order refresh actions', function*() {
        const expectedActions = [
          actions.orderRequest,
          actions.orderSuccess
        ];

        yield expect(actions.increaseRemorsePeriod(orderRef),
                     'to dispatch actions', expectedActions);
      });

    });

  });

});
