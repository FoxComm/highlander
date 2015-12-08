import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { orderSuccess } from './details.js';

const _createAction = (description, ...args) => {
  return createAction('ORDER_PAYMENT_METHOD_' + description, ...args);
};

const setError = _createAction('ERROR');

export const orderPaymentMethodRequest = _createAction('REQUEST');
export const orderPaymentMethodRequestSuccess = _createAction('REQUEST_SUCCESS');
export const orderPaymentMethodRequestFailed = _createAction('REQUEST_FAILED');
export const orderPaymentMethodStartEdit = _createAction('START_EDIT');
export const orderPaymentMethodStopEdit = _createAction('STOP_EDIT');

function deleteOrderPaymentMethod(path) {
  return dispatch => {
    return Api.delete(path)
      .then(order => dispatch(orderSuccess(order)))
      .catch(err => dispatch(setError(err)));
  };
}

export function deleteOrderGiftCardPayment(orderRefNum, code) {
  const path = `${basePath(orderRefNum)}/gift-cards/${code}`;
  return deleteOrderPaymentMethod(path);
}

export function deleteOrderStoreCreditPayment(orderRefNum) {
  const path = `${basePath(orderRefNum)}/store-credit`;
  return deleteOrderPaymentMethod(path);
}

export function deleteOrderCreditCardPayment(orderRefNum) {
  const path = `${basePath(orderRefNum)}/credit-cards`;
  return deleteOrderPaymentMethod(path);
}

function basePath(refNum) {
  return `/orders/${refNum}/payment-methods`;
};

const initialState = {
  isEditing: false,
  isFetching: false
};

const reducer = createReducer({
  [orderPaymentMethodRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [orderPaymentMethodRequestSuccess]: (state, payload) => {
    return {
      ...state,
      isFetching: false
    };
  },
  [orderPaymentMethodRequestFailed]: (state, err) => {
    console.error(err);
    return {
      ...state,
      isFetching: false
    };
  },
  [orderPaymentMethodStartEdit]: (state) => {
    return {
      ...state,
      isEditing: true
    };
  },
  [orderPaymentMethodStopEdit]: (state) => {
    return {
      ...state,
      isEditing: false
    };
  },
  [setError]: (state, err) => {
    return {
      ...state,
      err
    };
  },
}, initialState);

export default reducer;
