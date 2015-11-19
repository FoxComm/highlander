import _ from 'lodash';
import nock from 'nock';

const { reducer, ...actions } = importModule('customers/credit-cards.js', [
  'fetchCreditCards',
  'requestCustomerCreditCards',
  'receiveCustomerCreditCards'
]);

describe('customers credit cards module', function() {

  function creditCardsUrl(customerId) {
    return `/api/v1/customers/${customerId}/payment-methods/credit-cards`;
  }

  const creditCardPayload = require('../../../fixtures/customer-credit-cards.json');
  const customerId = 1;

  context('async actions', function() {
    before(function() {
      nock(phoenixUrl)
        .get(creditCardsUrl(customerId))
        .reply(200, creditCardPayload);
    });

    after(function() {
      nock.cleanAll();
    });

    it('fetchCreditCards', function*() {
      const expectedActions = [
        actions.requestCustomerCreditCards,
        actions.receiveCustomerCreditCards
      ];

      yield expect(actions.fetchCreditCards(customerId), 'to dispatch actions', expectedActions);
    });

  });

});
