import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { orderSuccess } from './details.js';
import { post } from '../../lib/search';
import { toQuery } from '../../elastic/common';
import createAsyncActions from '../async-utils';

const _createAction = (description, ...args) => {
  return createAction('ORDER_PAYMENT_METHOD_' + description, ...args);
};

const setError = _createAction('ERROR');

export const orderPaymentMethodStartEdit = _createAction('START_EDIT');
export const orderPaymentMethodStopEdit = _createAction('STOP_EDIT');
export const orderPaymentMethodStartAdd = _createAction('START_ADD');
export const orderPaymentMethodStopAdd = _createAction('STOP_ADD');

const orderPaymentMethodAddNewPaymentStart = _createAction('ADD_NEW_PAYMENT_START');
const orderPaymentMethodAddNewPaymentSuccess = _createAction('ADD_NEW_PAYMENT_SUCCESS');

function deleteOrderPaymentMethod(path) {
  return dispatch => {
    return Api.delete(path)
      .then(
        order => dispatch(orderSuccess(order)),
        err => dispatch(setError(err))
      );
  };
}

export function addOrderCreditCardPayment(orderRefNum, creditCardId) {
  return dispatch => {
    return Api.post(`${basePath(orderRefNum)}/credit-cards`, { creditCardId: creditCardId })
      .then(
        order => {
          dispatch(orderSuccess(order));
        },
        err => dispatch(setError(err))
      );
  };
}

export function createAndAddOrderCreditCardPayment(orderRefNum, creditCard, customerId) {
  return dispatch => {
    dispatch(orderPaymentMethodAddNewPaymentStart());

    const ccPayload = {
      isDefault: creditCard.isDefault,
      cardNumber: creditCard.cardNumber,
      holderName: creditCard.holderName,
      cvv: creditCard.cvv,
      expMonth: creditCard.expMonth,
      expYear: creditCard.expYear,
      addressId: creditCard.addressId,
    };

    return Api.post(`/customers/${customerId}/payment-methods/credit-cards`, ccPayload)
      .then(
        res => dispatch(addOrderCreditCardPayment(orderRefNum, res.id)),
        err => dispatch(setError(err))
      )
      .then(() => dispatch(orderPaymentMethodAddNewPaymentSuccess()));
  };
}

export function editCreditCardPayment(orderRefNum, creditCard, customerId) {
  const ccPayload = {
    isDefault: creditCard.isDefault,
    holderName: creditCard.holderName,
    expMonth: creditCard.expMonth,
    expYear: creditCard.expYear,
    addressId: _.get(creditCard, 'address.id', creditCard.addressId),
  };

  return dispatch => {
    return Api.patch(`/customers/${customerId}/payment-methods/credit-cards/${creditCard.id}`, ccPayload)
      .then(
        res => dispatch(addOrderCreditCardPayment(orderRefNum, res.id)),
        err => dispatch(setError(err))
      );
  };
}

export function addOrderStoreCreditPayment(orderRefNum, amount) {
  return dispatch => {
    dispatch(orderPaymentMethodAddNewPaymentStart());
    return Api.post(`${basePath(orderRefNum)}/store-credit`, { amount: amount })
      .then(
        order => {
          dispatch(orderPaymentMethodAddNewPaymentSuccess());
          dispatch(orderSuccess(order));
        },
        err => dispatch(setError(err))
      );
  };
}

export function addOrderGiftCardPayment(orderRefNum, code, amount) {
  return dispatch => {
    dispatch(orderPaymentMethodAddNewPaymentStart());
    return Api.post(`${basePath(orderRefNum)}/gift-cards`, { code: code, amount: amount })
      .then(
        order => {
          dispatch(orderPaymentMethodAddNewPaymentSuccess());
          dispatch(orderSuccess(order));
        },
        err => dispatch(setError(err))
      );
  };
}

const _giftCardSearch = createAsyncActions(
  'orders/giftCards',
  code => {
    const filters = [{
      term: 'code',
      operator: 'eq',
      value: {
        type: 'term',
        value: code,
      },
    }];

    return post('gift_cards_search_view/_search', toQuery(filters));
  }
);

export const giftCardSearch = _giftCardSearch.perform;


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
}

const initialState = {
  isAdding: false,
  isEditing: false,
  isFetching: false,
  isUpdating: false,
  isSearchingGiftCards: false,
  giftCards: [],
};

const reducer = createReducer({
  [orderPaymentMethodStartEdit]: (state) => {
    return {
      ...state,
      isEditing: true
    };
  },
  [orderPaymentMethodStopEdit]: (state) => {
    return {
      ...state,
      isAdding: false,
      isEditing: false
    };
  },
  [orderPaymentMethodStartAdd]: (state) => {
    return {
      ...state,
      isAdding: true,
    };
  },
  [orderPaymentMethodStopAdd]: (state) => {
    return {
      ...state,
      isAdding: false,
    };
  },
  [orderPaymentMethodAddNewPaymentStart]: (state) => {
    return {
      ...state,
      isUpdating: true,
    };
  },
  [orderPaymentMethodAddNewPaymentSuccess]: (state) => {
    return initialState;
  },
  [_giftCardSearch.succeeded]: (state, payload) => {
    return {
      ...state,
      giftCards: payload.result,
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
