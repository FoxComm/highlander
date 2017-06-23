import nock from 'nock';
import sinon from 'sinon';
import stripe from 'lib/stripe';

const actions = importSource('modules/customers/credit-cards.js', [
  'fetchCreditCards',
  'requestCustomerCreditCards',
  'receiveCustomerCreditCards',
  'createCreditCard',
  'closeNewCustomerCreditCard',
  'saveCreditCard',
  'closeEditCustomerCreditCard',
  'confirmCreditCardDeletion',
  'closeDeleteCustomerCreditCard',
  'creditCardsUrl',
  'creditCardUrl',
  'creditCardDefaultUrl',
]);

describe('customers credit cards module', function() {
  function creditCardsUrl(customerId) {
    return `/api/v1/customers/${customerId}/payment-methods/credit-cards`;
  }

  function creditCardUrl(customerId, cardId) {
    return `/api/v1/customers/${customerId}/payment-methods/credit-cards/${cardId}`;
  }

  const creditCardPayload = require('../../../fixtures/customer-credit-cards.json');
  const customerId = 1;

  context('helper functions', function() {
    const customerId = Math.floor(Math.random() * 10) + 1;
    const cardId = Math.floor(Math.random() * 10) + 1;

    it('creditCardsUrl should construct valid url', function() {
      const expected = `/customers/${customerId}/payment-methods/credit-cards`;
      expect(actions.creditCardsUrl(customerId)).to.be.equal(expected);
    });

    it('creditCardUrl should construct valid url', function() {
      const expected = `/customers/${customerId}/payment-methods/credit-cards/${cardId}`;
      expect(actions.creditCardUrl(customerId, cardId)).to.be.equal(expected);
    });

    it('creditCardDefaultUrl should construct valid url', function() {
      const expected = `/customers/${customerId}/payment-methods/credit-cards/${cardId}/default`;
      expect(actions.creditCardDefaultUrl(customerId, cardId)).to.be.equal(expected);
    });
  });

  context('async actions', function() {
    before(function() {
      nock(process.env.API_URL).get(creditCardsUrl(customerId)).reply(200, creditCardPayload);
    });

    after(function() {
      nock.cleanAll();
    });

    it('fetchCreditCards', function*() {
      const expectedActions = [actions.requestCustomerCreditCards, actions.receiveCustomerCreditCards];

      yield expect(actions.fetchCreditCards(customerId), 'to dispatch actions', expectedActions);
    });

    context('createCreditCard', function() {
      const payload = {
        holderName: 'Yax',
        number: '4242424242424242',
        expMonth: 11,
        expYear: 2017,
        isDefault: false,
        addressId: 1,
      };

      const stub = sinon.stub(stripe, 'addCreditCard');
      stub.returns(Promise.resolve());

      beforeEach(function() {
        const uri = creditCardsUrl(customerId);
        nock(process.env.API_URL).get(uri).reply(200, creditCardPayload).post(uri).reply(201, payload);
      });

      const initialState = {
        customers: {
          creditCards: {
            [customerId]: {
              newCreditCard: payload,
            },
          },
          addresses: {
            [customerId]: {
              addresses: [{ id: payload.addressId }],
            },
          },
        },
      };

      it('should close form and fetch customers', function*() {
        const expectedActions = [
          actions.requestCustomerCreditCards,
          actions.closeNewCustomerCreditCard,
          actions.receiveCustomerCreditCards,
        ];

        yield expect(actions.createCreditCard(customerId), 'to dispatch actions', expectedActions, initialState);
      });
    });

    context('saveCreditCard', function() {
      const payload = {
        holderName: 'Yax',
        number: '4242424242424242',
        expMonth: 11,
        expYear: 2017,
        isDefault: false,
        addressId: 1,
        id: 10,
      };

      beforeEach(function() {
        nock(process.env.API_URL)
          .get(creditCardsUrl(customerId))
          .reply(200, creditCardPayload)
          .patch(creditCardUrl(customerId, payload.id))
          .reply(200, payload);
      });

      const initialState = {
        customers: {
          creditCards: {
            [customerId]: {
              cards: [],
              editingCreditCard: payload,
              editingId: payload.id,
            },
          },
        },
      };

      it('should close form and fetch customers', function*() {
        const expectedActions = [
          actions.requestCustomerCreditCards,
          actions.closeEditCustomerCreditCard,
          actions.receiveCustomerCreditCards,
        ];

        yield expect(actions.saveCreditCard(customerId), 'to dispatch actions', expectedActions, initialState);
      });
    });

    context('confirmCreditCardDeletion', function() {
      beforeEach(function() {
        nock(process.env.API_URL)
          .get(creditCardsUrl(customerId))
          .reply(200, creditCardPayload)
          .delete(creditCardUrl(customerId, 10))
          .reply(204);
      });

      const initialState = {
        customers: {
          creditCards: {
            [customerId]: {
              cards: creditCardPayload,
              deletingId: 10,
            },
          },
        },
      };

      it('should close form and fetch customers', function*() {
        const expectedActions = [
          actions.requestCustomerCreditCards,
          actions.closeDeleteCustomerCreditCard,
          actions.receiveCustomerCreditCards,
        ];

        yield expect(
          actions.confirmCreditCardDeletion(customerId, 10),
          'to dispatch actions',
          expectedActions,
          initialState
        );
      });
    });
  });
});
