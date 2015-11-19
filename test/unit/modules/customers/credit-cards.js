import _ from 'lodash';
import nock from 'nock';
import thunk from 'redux-thunk';

const { reducer, ...actions } = importModule('customers/credit-cards.js', [
  'fetchCreditCards',
  'requestCustomerCreditCards',
  'receiveCustomerCreditCards',
  'createCreditCard',
  'closeNewCustomerCreditCard'
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

    context('createCreditCard', function() {
      const payload = {
        holderName: 'Yax',
        number: '4242424242424242',
        cvv: '123',
        expMonth: 11,
        expYear: 2017,
        isDefault: false,
        addressId: 1
      };

      beforeEach(function() {
        const uri = creditCardsUrl(customerId);
        nock(phoenixUrl)
          .get(uri)
          .reply(200, creditCardPayload)
          .post(uri)
          .reply(201, payload);
      });

      const initialState = {
        [customerId]: {
          cards: [],
          newCreditCard: payload
        }
      };

      it('should close form and fetch customers', function*() {
        const expectedActions = [
          actions.requestCustomerCreditCards,
          actions.closeNewCustomerCreditCard,
          actions.receiveCustomerCreditCards
        ];

        yield expect(actions.createCreditCard(customerId), 'to dispatch actions', expectedActions, initialState);
      });

    });

  });

});
