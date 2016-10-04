import _ from 'lodash';
import { assoc, dissoc, update, merge } from 'sprout-data';
import Api from 'lib/api';
import stripe from 'lib/stripe';
import { getBillingAddress } from 'lib/credit-card-utils';
import { createAction, createReducer } from 'redux-act';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_CREDIT_CARD_' + description, ...args);
};

// State change reducers
export const newCustomerCreditCard = _createAction('NEW');
export const closeNewCustomerCreditCard = _createAction('NEW_CLOSE');
export const changeNewCustomerCreditCardFormData = _createAction('NEW_CHANGE_FORM',
  (id, name, value) => [id, name, value]);
export const editCustomerCreditCard = _createAction('EDIT',
  (customerId, cardId) => [customerId, cardId]);
export const closeEditCustomerCreditCard = _createAction('EDIT_CLOSE',
  (customerId, cardId) => [customerId, cardId]);
export const changeEditCustomerCreditCardFormData = _createAction('EDIT_CHANGE_FORM',
  (id, name, value) => [id, name, value]);
export const deleteCustomerCreditCard = _createAction('DELETE',
  (customerId, cardId) => [customerId, cardId]);
export const closeDeleteCustomerCreditCard = _createAction('DELETE_CLOSE');

// API reducers
const receiveCustomerCreditCards = _createAction('RECEIVE',
  (id, cards) => [id, cards]);
const requestCustomerCreditCards = _createAction('REQUEST');
const failCustomerCreditCards = _createAction('FAIL',
  (id, err, source) => [id, err]);

function creditCardsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/credit-cards`;
}

function creditCardUrl(customerId, cardId) {
  return `/customers/${customerId}/payment-methods/credit-cards/${cardId}`;
}

function creditCardDefaultUrl(customerId, cardId) {
  return `/customers/${customerId}/payment-methods/credit-cards/${cardId}/default`;
}

function fetchForCustomer(id, dispatch) {
  Api.get(creditCardsUrl(id))
    .then(
      cards => dispatch(receiveCustomerCreditCards(id, cards)),
      err => dispatch(failCustomerCreditCards(id, err))
    );
}

function setDefaultCreditCard(customerId, cardId, dispatch) {
  const payload = { isDefault: true };

  return Api.post(creditCardDefaultUrl(customerId, cardId), payload)
    .catch(err => dispatch(failCustomerCreditCards(customerId, err)));
}

function resetDefaultCreditCard(customerId, cardId, currentDefaultId, dispatch) {
  const resetPayload = { isDefault: false };

  return Api.post(creditCardDefaultUrl(customerId, currentDefaultId), resetPayload)
    .then(() => setDefaultCreditCard(customerId, cardId, dispatch))
    .catch(err => dispatch(failCustomerCreditCards(customerId, err)));
}

export function fetchCreditCards(id) {
  return dispatch => {
    dispatch(requestCustomerCreditCards(id));

    fetchForCustomer(id, dispatch);
  };
}

export function createCreditCard(customerId) {
  return (dispatch, getState) => {
    dispatch(requestCustomerCreditCards(customerId));
    const state = getState();

    const cards = _.get(state, 'customers.creditCards', {});
    const cardData = _.get(cards, [customerId, 'newCreditCard'], {});
    const cardsArray = _.get(cards, [customerId, 'cards']);
    const currentDefault = _.find(cardsArray, card => card.isDefault);
    const isDefault = cardData.isDefault;

    const handleSuccess = () => {
      dispatch(closeNewCustomerCreditCard(customerId));
      fetchForCustomer(customerId, dispatch);
    };

    stripe.addCreditCard(cardData, getBillingAddress(getState, customerId, cardData.addressId), false)
      .then(creditCard => Api.post(creditCardsUrl(customerId), creditCard))
      .then(card => {
        if (isDefault && currentDefault) {
          resetDefaultCreditCard(customerId, card.id, currentDefault.id, dispatch).then(handleSuccess);
        } else if (isDefault && _.isEmpty(currentDefault)) {
          setDefaultCreditCard(customerId, card.id, dispatch).then(handleSuccess);
        } else {
          handleSuccess();
        }
      })
      .catch(err => dispatch(failCustomerCreditCards(customerId, err)));
  };
}

export function confirmCreditCardDeletion(id) {
  return (dispatch, getState) => {
    dispatch(requestCustomerCreditCards(id));

    const cards = _.get(getState(), 'customers.creditCards', {});
    const creditCardId = _.get(cards, [id, 'deletingId']);
    Api.delete(creditCardUrl(id, creditCardId))
      .then(() => {
        dispatch(closeDeleteCustomerCreditCard(id));
        fetchForCustomer(id, dispatch);
      })
      .catch(err => dispatch(failCustomerCreditCards(id, err)));
  };
}

export function saveCreditCard(customerId) {
  return (dispatch, getState) => {
    dispatch(requestCustomerCreditCards(customerId));

    const cards = _.get(getState(), 'customers.creditCards', {});
    const creditCardId = _.get(cards, [customerId, 'editingId']);
    const cardData = _.get(cards, [customerId, 'editingCreditCard']);
    const cardsArray = _.get(cards, [customerId, 'cards']);
    const currentDefault = _.find(cardsArray, card => card.isDefault);
    const isDefault = cardData.isDefault;
    const billingAddress = getBillingAddress(getState, customerId, cardData.addressId);

    cardData.billingAddress = {
      address: billingAddress,
      id: billingAddress.id,
    };

    const handleSuccess = () => {
      dispatch(closeEditCustomerCreditCard(customerId));
      fetchForCustomer(customerId, dispatch);
    };

    Api.patch(creditCardUrl(customerId, creditCardId), cardData)
      .then(card => {
        if (isDefault && currentDefault && currentDefault.id !== creditCardId) {
          resetDefaultCreditCard(customerId, card.id, currentDefault.id, dispatch).then(handleSuccess);
        } else if (isDefault && _.isEmpty(currentDefault)) {
          setDefaultCreditCard(customerId, card.id, dispatch).then(handleSuccess);
        } else {
          handleSuccess();
        }
      })
      .catch(err => dispatch(failCustomerCreditCards(customerId, err)));
  };
}

export function toggleDefault(customerId, creditCardId) {
  return (dispatch, getState) => {
    dispatch(requestCustomerCreditCards(customerId));

    const cards = _.get(getState(), 'customers.creditCards', {});
    const cardsArray = _.get(cards, [customerId, 'cards']);
    const currentDefault = _.find(cardsArray, card => card.isDefault);
    const card = _.find(cardsArray, card => card.id === creditCardId);
    const payload = { isDefault: !card.isDefault };

    const errHandler = err => dispatch(failCustomerCreditCards(customerId, err));

    if (!_.isEmpty(currentDefault)) {
      const currentDefaultId = currentDefault.id;
      const resetPayload = { isDefault: false };

      Api.post(creditCardDefaultUrl(customerId, currentDefaultId), resetPayload)
        .then(() => Api.post(creditCardDefaultUrl(customerId, creditCardId), payload), errHandler)
        .then(() => fetchForCustomer(customerId, dispatch), errHandler);
    } else {
      Api.post(creditCardDefaultUrl(customerId, creditCardId), payload)
        .then(() => fetchForCustomer(customerId, dispatch), errHandler);
    }
  };
}

const initialState = {};

const reducer = createReducer({
  [newCustomerCreditCard]: (state, id) => {
    return update(state, id, merge, {
      newCreditCard: {
        isDefault: false,
        holderName: '',
        cardNumber: '',
        cvv: '',
        expMonth: 0,
        expYear: 0,
        addressId: ''
      }
    });
  },
  [closeNewCustomerCreditCard]: (state, id) => {
    return dissoc(state, [id, 'newCreditCard']);
  },
  [changeNewCustomerCreditCardFormData]: (state, [id, name, value]) => {
    return assoc(state, [id, 'newCreditCard', name], value);
  },
  [editCustomerCreditCard]: (state, [customerId, cardId]) => {
    const cards = _.get(state, [customerId, 'cards'], []);
    const creditCard = _.find(cards, card => cardId === card.id);

    return update(state, customerId, merge, {
      editingId: cardId,
      editingCreditCard: {
        ...creditCard,
        addressId: null
      }
    });
  },
  [changeEditCustomerCreditCardFormData]: (state, [id, name, value]) => {
    return assoc(state, [id, 'editingCreditCard', name], value);
  },
  [closeEditCustomerCreditCard]: (state, [customerId, cardId]) => {
    return dissoc(state, [customerId, 'editingId'], [customerId, 'editingCreditCard']);
  },
  [deleteCustomerCreditCard]: (state, [customerId, cardId]) => {
    return assoc(state, [customerId, 'deletingId'], cardId);
  },
  [closeDeleteCustomerCreditCard]: (state, id) => {
    return dissoc(state, [id, 'deletingId']);
  },
  [requestCustomerCreditCards]: (state, id) => {
    return assoc(state, [id, 'isFetching'], true);
  },
  [receiveCustomerCreditCards]: (state, [id, payload]) => {
    // fallback to plain array
    const cards = _.get(payload, 'result', payload);
    const sortedCards = _.sortBy(cards, 'id');

    return assoc(state,
      [id, 'cards'], sortedCards,
      [id, 'isFetching'], false,
      [id, 'err'], null
    );
  },
  [failCustomerCreditCards]: (state, [id, err]) => {
    console.error(err);
    return assoc(state, [id, 'err'], err, [id, 'isFetching'], false);
  }
}, initialState);

export default reducer;
