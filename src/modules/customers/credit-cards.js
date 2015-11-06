'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

// State change reducers
export const newCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARD_NEW');
export const closeNewCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARD_NEW_CLOSE');
export const changeNewCustomerCreditCardFormData = createAction('CUSTOMER_CREDIT_CARD_NEW_CHANGE_FORM', (id, name, value) => [id, name, value]);
export const editCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARDS_EDIT', (customerId, cardId) => [customerId, cardId]);
export const cancelEditCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARDS_EDIT_CANCEL', (customerId, cardId) => [customerId, cardId]);
export const deleteCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARDS_DELETE', (customerId, cardId) => [customerId, cardId]);
export const closeDeleteCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARDS_DELETE_CLOSE');

// API reducers
const receiveCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_RECEIVE', (id, cards) => [id, cards]);
const requestCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_REQUEST');
const failCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_FAIL', (id, err, source) => [id, err]);

function fetchForCustomer(id, dispatch) {
  Api.get(`/customers/${id}/payment-methods/credit-cards`)
    .then(cards => dispatch(receiveCustomerCreditCards(id, cards)))
    .catch(err => dispatch(failCustomerCreditCards(id, err)));
}

export function fetchCreditCards(id) {
  return dispatch => {
    dispatch(requestCustomerCreditCards(id));

    fetchForCustomer(id, dispatch);
  };
}

export function createCreditCard(id) {
  return (dispatch, getState) => {
    dispatch(requestCustomerCreditCards(id));

    const cards = _.get(getState(), 'customers.creditCards', {});
    const cardData = _.get(cards, `${id}.newCreditCard`, {});
    console.log(cardData);
    Api.post(`/customers/${id}/payment-methods/credit-cards`, cardData)
      .then(() => {
        dispatch(closeNewCustomerCreditCard(id));
        fetchForCustomer(id, dispatch);
      })
      .catch(err => dispatch(failCustomerCreditCards(id, err)));

  };
}

export function confirmCreditCardDeletion(id) {
  return (dispatch, getState) => {
    dispatch(requestCustomerCreditCards(id));

    const cards = _.get(getState(), 'customers.creditCards', {});
    const creditCardId = _.get(cards, `${id}.deletingId`);
    Api.delete(`/customers/${id}/payment-methods/credit-cards/${creditCardId}`)
      .then(() => {
        dispatch(closeDeleteCustomerCreditCard(id));
        fetchForCustomer(id, dispatch);
      })
      .catch(err => dispatch(failCustomerCreditCards(id, err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [newCustomerCreditCard]: (state, id) => {
    console.log("newCustomerCreditCard");
    return {
      ...state,
      [id]: {
        ...state[id],
        newCreditCard: {
          isDefault: false,
          holderName: null,
          number: null,
          cvv: null,
          expMonth: null,
          expYear: null,
          addressId: 1
        }
      }
    };
  },
  [closeNewCustomerCreditCard]: (state, id) => {
    console.log("cancelNewCustomerCreditCard");
    const { newCreditCard, ...restState } = state[id];
    return {
      ...state,
      [id]: {
        ...restState
      }
    };
  },
  [changeNewCustomerCreditCardFormData]: (state, [id, name, value]) => {
    console.log(name);
    console.log(value);
    const newCreditCard = _.get(state, [id, 'newCreditCard']);
    console.log(newCreditCard);
    const newState = {
      ...state,
      [id]: {
        ...state[id],
        newCreditCard: {
          ...newCreditCard,
          [name]: value
        }
      }
    };

    return newState;
  },
  [editCustomerCreditCard]: (state, [customerId, cardId]) => {
    console.log('editCustomerCreditCard');
    return {
      ...state,
      [customerId]: {
        ...state[customerId],
        editingId: cardId
      }
    };
  },
  [cancelEditCustomerCreditCard]: (state, [customerId, cardId]) => {
    console.log('cancelEditCustomerCreditCard');
    return {
      ...state,
      [customerId]: {
        ...state[customerId],
        editingId: null
      }
    };
  },
  [deleteCustomerCreditCard]: (state, [customerId, cardId]) => {
    console.log('deleteCustomerCreditCard');
    return {
      ...state,
      [customerId]: {
        ...state[customerId],
        deletingId: cardId
      }
    };
  },
  [closeDeleteCustomerCreditCard]: (state, id) => {
    console.log('closeDeleteCustomerCreditCard');
    return {
      ...state,
      [id]: {
        ...state[id],
        deletingId: null
      }
    };
  },
  [requestCustomerCreditCards]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetching: true
      }
    };
  },
  [receiveCustomerCreditCards]: (state, [id, payload]) => {
    const cards = _.get(payload, 'result', []);
    return {
      ...state,
      [id]: {
        ...state[id],
        cards,
        isFetching: false
      }
    };
  },
  [failCustomerCreditCards]: (state, [id, err]) => {
    console.error(err);

    return {
      ...state,
      [id]: {
        ...state[id],
        err,
        isFetching: false
      }
    };
  },
}, initialState);

export default reducer;
