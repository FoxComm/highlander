'use strict';

import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

const receiveCustomer = createAction('CUSTOMER_RECEIVE', (id, customer) => [id, customer]);
const failCustomer = createAction('CUSTOMER_FAIL', (id, err, source) => [id, err, source]);
const requestCustomer = createAction('CUSTOMER_REQUEST');
const updateCustomer = createAction('CUSTOMER_UPDATED', (id, customer) => [id, customer]);
const receiveCustomerAdresses = createAction('CUSTOMER_ADDRESSES_RECEIVE', (id, addresses) => [id, addresses]);
const receiveCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_RECEIVE', (id, cards) => [id, cards]);
const requestCustomerAdresses = createAction('CUSTOMER_ADDRESSES_REQUEST');
const requestCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_REQUEST');

export function fetchCustomer(id) {
  return dispatch => {
    dispatch(requestCustomer(id));
    Api.get(`/customers/${id}`)
      .then(customer => dispatch(receiveCustomer(id, customer)))
      .catch(err => dispatch(failCustomer(id, err, fetchCustomer)));
  };
}

export function editCustomer(id, data) {
  return dispatch => {
    Api.patch(`/customers/${id}`, data)
      .then(customer => dispatch(updateCustomer(id, customer)))
      .catch(err => dispatch(failCustomer(id, err, editCustomer)));
  };
}

export function fetchAdresses(id) {
  return dispatch => {
    dispatch(requestCustomerAdresses(id));

    Api.get(`/customers/${id}/addresses`)
      .then(addresses => dispatch(receiveCustomerAdresses(id, addresses)))
      .catch(err => dispatch(failCustomer(id, err, fetchCustomer)));
  };
}

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
  [requestCustomer]: (entries, id) => {
    return {
      ...entries,
      [id]: {
        ...entries[id],
        isFetching: true,
        err: null
      }
    };
  },
  [receiveCustomer]: (state, [id, details]) => {
    return {
      ...state,
      [id]: {
        err: null,
        isFetching: false,
        details: haveType(details, 'customer')
      }
    };
  },
  [failCustomer]: (state, [id, err, source]) => {
    console.error(err);

    return {
      ...state,
      [id]: {
        ...state[id],
        err,
        ...(
          source === fetchCustomer ? {isFetching: false} : {}
        )
      }
    };
  },
  [updateCustomer]: (state, [id, details]) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        err: null,
        details
      }
    };
  },
  [requestCustomerAdresses]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetchingAddresses: true
      }
    };
  },
  [requestCustomerCreditCards]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetchingCards: true
      }
    };
  },
  [receiveCustomerAdresses]: (state, [id, addresses]) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetchingAddresses: false,
        addresses
      }
    };
  },
  [receiveCustomerCreditCards]: (state, [id, cards]) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetchingCards: false,
        cards
      }
    };
  }
}, initialState);

export default reducer;
