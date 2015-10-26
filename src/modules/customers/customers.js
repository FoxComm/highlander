'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

const requestCustomers = createAction('CUSTOMERS_REQUEST');
const receiveCustomers = createAction('CUSTOMERS_RECEIVE');
const updateCustomers = createAction('CUSTOMERS_UPDATE');
const failCustomers = createAction('CUSTOMERS_FAIL', (err, source) => [err, source]);

export function fetchCustomers() {
  return dispatch => {
    dispatch(requestCustomers());
    return Api.get('/customers')
      .then(customers => dispatch(receiveCustomers(customers)))
      .catch(err => dispatch(failCustomers(err, fetchCustomers)));
  };
}

function shouldFetchCustomers(state) {
  const customers = state.customers;
  if (!customers) {
    return true;
  } else if (customers.isFetching) {
    return false;
  }
  return customers.didInvalidate;
}

export function fetchCustomersIfNeeded() {
  return (dispatch, getState) => {
    if (shouldFetchCustomers(getState())) {
      return dispatch(fetchCustomers());
    }
  };
}

function updateItems(items, newItems) {
  return _.values({
    ..._.indexBy(items, 'id'),
    ..._.indexBy(newItems, 'id')
  });
}

const initialState = {
  isFetching: false,
  didInvalidate: true,
  items: []
};

const reducer = createReducer({
  [requestCustomers]: (state) => {
    return {
      ...state,
      isFetching: true,
      didInvalidate: false
    };
  },
  [receiveCustomers]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      didInvalidate: false,
      items: payload
    };
  },
  [updateCustomers]: (state, payload) => {
    return {
      ...state,
      items: updateItems(state.items, payload)
    };
  },
  [failCustomers]: (state, [err, source]) => {
    console.error(err);

    if (source === fetchCustomers) {
      return {
        ...state,
        isFetching: false,
        didInvalidate: false
      };
    }

    return state;
  }
}, initialState);

export default reducer;
