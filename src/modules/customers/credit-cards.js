'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

// State change reducers
export const newCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARD_NEW');
export const cancelNewCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARD_NEW_CANCEL');
export const editCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARDS_EDIT', (customerId, cardId) => [customerId, cardId]);
export const cancelEditCustomerCreditCard = createAction('CUSTOMER_CREDIT_CARDS_EDIT_CANCEL', (customerId, cardId) => [customerId, cardId]);

// API reducers
const receiveCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_RECEIVE', (id, cards) => [id, cards]);
const requestCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_REQUEST');
const failCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_FAIL', (id, err, source) => [id, err, source]);

export function fetchCreditCards(id) {
  return dispatch => {
    dispatch(requestCustomerCreditCards(id));

    Api.get(`/customers/${id}/payment-methods/credit-cards`)
      .then(cards => dispatch(receiveCustomerCreditCards(id, cards)))
      .catch(err => dispatch(failCustomer(id, err, fetchCustomer)));
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
        newCreditCard: {}
      }
    };
  },
  [cancelNewCustomerCreditCard]: (state, id) => {
    console.log("cancelNewCustomerCreditCard");
    const { newCreditCard, ...restState } = state[id];
    return {
      ...state,
      [id]: {
        ...restState
      }
    };
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
