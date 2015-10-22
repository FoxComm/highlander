'use strict';

import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

export const receiveCustomer = createAction('CUSTOMER_RECEIVE', (id, customer) => [id, customer]);
export const failCustomer = createAction('CUSTOMER_FAIL', (id, err, source) => [id, err, source]);
export const requestCustomer = createAction('CUSTOMER_REQUEST');
export const updateCustomer = createAction('CUSTOMER_UPDATED', (id, customer) => [id, customer]);

function shouldFetchCustomer(id, state) {
  const entry = state.customers.details[id];
  if (!entry) {
    return true;
  } else if (entry.isFetching) {
    return false;
  }
  return entry.didInvalidate;
}

export function fetchCustomer(id) {
  return dispatch => {
    dispatch(requestCustomer(id));
    Api.get(`/customers/${id}`)
      .then(customer => dispatch(receiveCustomer(id, customer)))
      .catch(err => dispatch(failCustomer(id, err, fetchCustomer)));
  };
}

export function fetchCustomerIfNeeded(id) {
  return (dispatch, getState) => {
    if (shouldFetchCustomer(id, getState())) {
      dispatch(fetchCustomer(id));
    }
  };
}

export function editCustomer(id, data) {
  return dispatch => {
    Api.patch(`/customers/${id}`, data)
      .then(customer => dispatch(updateCustomer(id, customer)))
      .catch(err => dispatch(failCustomer(id, err, editCustomer)));
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
        didInvalidate: false,
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
        didInvalidate: false,
        details
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
          source === fetchCustomer ? {
            isFetching: false,
            didInvalidate: false} : {}
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
  }
}, initialState);

export default reducer;
