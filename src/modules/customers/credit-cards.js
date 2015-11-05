'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

// State change reducers
export const newCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARD_NEW');
export const closeNewCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARD_NEW_CANCEL');
export const changeNewCustomerCreditCardFormData = createAction('CUSTOMER_CREDIT_CARD_NEW_CHANGE_FORM', (id, name, value) => [id, name, value]);
export const editCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARDS_EDIT', (customerId, cardId) => [customerId, cardId]);
export const cancelEditCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARDS_EDIT_CANCEL', (customerId, cardId) => [customerId, cardId]);

// API reducers
const receiveCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_RECEIVE', (id, cards) => [id, cards]);
const requestCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_REQUEST');
const failCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_FAIL', (id, err, source) => [id, err, source]);

function fetchForCustomer(id, dispatch) {
  Api.get(`/customers/${id}/payment-methods/credit-cards`)
    .then(cards => dispatch(receiveCustomerCreditCards(id, cards)))
    .catch(err => dispatch(failCustomer(id, err, fetchCustomer)));
}

export function fetchCreditCards(id) {
  return dispatch => {
    dispatch(requestCustomerCreditCards(id));

    fetchForCustomer(id, dispatch);
  };
}

export function createCreditCard(id) {
  return dispatch => {
    dispatch(requestCustomerCreditCards(id));

    // ToDo: submit form here
    dispatch(closeNewCustomerCreditCard(id));
    fetchForCustomer(id, dispatch);
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
          name: null,
          cardNumber: null,
          cvv: null,
          expitationMonth: null,
          expitationYear: null,
          billingAddressId: 1
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
      [id]: {
        ...state[id],
        editingId: cardId
      }
    };
  },
  [cancelEditCustomerCreditCard]: (state, [customerId, cardId]) => {
    console.log('cancelEditCustomerCreditCard');
    return {
      ...state,
      [id]: {
        ...state[id],
        editingId: null
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
  [failCustomerCreditCards]: (state, [id, err, source]) => {
    console.error(err);

    return {
      ...state,
      [id]: {
        ...state[id],
        err,
        ...(
          source === requestCustomerCreditCards ? {isFetching: false} : {}
        )
      }
    };
  },
}, initialState);

export default reducer;
