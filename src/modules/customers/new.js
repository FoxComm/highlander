import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
import { transitionTo } from '../../route-helpers';


export const changeFormData = createAction('CUSTOMER_NEW_CHANGE_FORM', (name, value) => [name, value]);
export const submitCustomer = createAction('CUSTOMER_SUMBIT');
export const openCustomerDetails = createAction('CUSTOMER_OPEN_DETAILS');
export const resetForm = createAction('CUSTOMER_RESET_FORM');
const failNewCustomer = createAction('CUSTOMER_NEW_FAIL', (err, source) => [err, source]);

export function createCustomer(history) {
  return (dispatch, getState) => {
    const customerNew = getState().customers.adding;
    dispatch(submitCustomer());

    Api.post('/customers', customerNew)
      .then(
        data => {
          dispatch(openCustomerDetails(data));
          transitionTo(history, 'customer', {customerId: data.id});
        },
        err => dispatch(failNewCustomer(err))
      );
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
    return assoc(state, name, value);
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
  [resetForm]: (state) => {
    return initialState;
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
