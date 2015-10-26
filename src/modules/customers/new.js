'use strict';

// state for customer adding form

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

export const changeFormData = createAction('CUSTOMER_NEW_CHANGE_FORM', (name, value) => [name, value]);
export const submitCustomer = createAction('CUSTOMER_SUMBIT');
export const openCustomerDetails = createAction('CUSTOMER_OPEN_DETAILS');
export const failNewCustomer = createAction('CUSTOMER_NEW_FAIL', (err, source) => [err, source]);

export function createCustomer() {
  return (dispatch, getState) => {
    const customerNew = getState().customers.adding;
    dispatch(submitCustomer());

    Api.post('/customers', customerNew)
      .then(data => dispatch(openCustomerDetails(data)))
      .catch(err => dispatch(failNewCustomer(err)));
  };
}

const initialState = {
  id: null,
  email: null,
  name: null,
  isFetching: false
};

const reducer = createReducer({
  [changeFormData]: (state, [name, value]) => {
    const newState = {
      ...state,
      [name]: value
    };

    return newState;
  },
  [submitCustomer]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [openCustomerDetails]: (state, payload) => {
    return {
      ...state,
      id: payload.id,
      isFetching: false
    };
  },
  [failNewCustomer]: (state, [err, source]) => {
    console.error(err);

    return {
      ...state,
      isFetching: false
    };
  }
}, initialState);

export default reducer;
