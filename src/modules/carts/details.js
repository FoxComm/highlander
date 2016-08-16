
import _ from 'lodash';
import Api from 'lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
import OrderParagon from 'paragons/order';

import createAsyncActions from 'modules/async-utils';

const checkoutRequest = createAction('ORDER_CHECKOUT_REQUEST');
const checkoutSuccess = createAction('ORDER_CHECKOUT_SUCCESS');
const checkoutFailed = createAction('ORDER_CHECKOUT_FAILURE');

////////////////////////////////////////////////////////////////////////////////
// Cart Manipulation Actions

const _fetchCart = createAsyncActions(
  'fetchCart',
  (refNum: string) => Api.get(`/carts/${refNum}`)
);

const _fetchCustomerCart = createAsyncActions(
  'fetchCustomerCart',
  (customerId: number) => Api.get(`/customers/${customerId}/cart`)
);

export function fetchCart(refNum: string) {
  return dispatch => dispatch(_fetchCart.perform(refNum));
}

export function fetchCustomerCart(customerId: number) {
  return dispatch => dispatch(_fetchCustomerCart.perform(customerId));
}

////////////////////////////////////////////////////////////////////////////////
// Line Item Actions

const _updateLineItemCount = createAsyncActions(
  'updateLineItemCount',
  (refNum: string, payload: Object) => Api.post(`/orders/${refNum}/line-items`, payload)
);

export function updateLineItemCount(refNum: string, sku: string, quantity: number) {
  const payload = [{ sku, quantity }];
  return dispatch => dispatch(_updateLineItemCount.perform(refNum, payload));
}

export function deleteLineItem(refNum: string, sku: string) {
  const payload = [{ sku, quantity: 0 }]
  return dispatch => dispatch(_updateLineItemCount.perform(refNum, payload));
}

////////////////////////////////////////////////////////////////////////////////
// Shipping Method Actions

const _updateShippingMethod = createAsyncActions(
  'updateShippingMethod',
  (refNum: string, shippingMethodId: number) => {
    const payload = { shippingMethodId };
    return Api.patch(`/orders/${refNum}/shipping-method`, payload);
  }
);

export function updateShippingMethod(refNum: string, shippingMethodId: number) {
  return dispatch => dispatch(_updateShippingMethod.perform(refNum, shippingMethodId));
}


export function checkout(refNum) {
  return dispatch => {
    dispatch(checkoutRequest());
    return Api.post(`/orders/${refNum}/checkout`)
      .then(
        order => {
          dispatch(cartSuccess(order));
          dispatch(checkoutSuccess());
        },
        err => dispatch(checkoutFailed(err))
      );
  };
}

function parseMessages(messages, state) {
  return _.reduce(messages, (results, message) => {
    if (message.indexOf('items') != -1) {
      return { ...results, itemsStatus: state };
    } else if (message.indexOf('empty cart') != -1) {
      return { ...results, itemsStatus: state };
    } else if (message.indexOf('shipping address') != -1) {
      return { ...results, shippingAddressStatus: state };
    } else if (message.indexOf('shipping method') != -1) {
      return { ...results, shippingMethodStatus: state };
    } else if (message.indexOf('payment method') != -1) {
      return { ...results, paymentMethodStatus: state };
    } else if (message.indexOf('insufficient funds') != -1) {
      return { ...results, paymentMethodStatus: state };
    }

    return results;
  }, {});
}


const initialState = {
  isCheckingOut: false,
  cart: {},
  validations: {
    errors: [],
    warnings: [],
    itemsStatus: 'success',
    shippingAddressStatus: 'success',
    shippingMethodStatus: 'success',
    paymentMethodStatus: 'success'
  }
};

function receiveCart(state, payload) {
  const order = _.get(payload, 'result', payload);
  const errors = _.get(payload, 'errors', []);
  const warnings = _.get(payload, 'warnings', []);

  // Initial state (assume in good standing)
  const status = {
    itemsStatus: 'success',
    shippingAddressStatus: 'success',
    shippingMethodStatus: 'success',
    paymentMethodStatus: 'success',

    // Find warnings
    ...parseMessages(warnings, 'warning'),

    // Find errors
    ...parseMessages(errors, 'error')
  };

  return {
    ...state,
    cart: new OrderParagon(order),
    validations: {
      errors: errors,
      warnings: warnings,
      ...status
    }
  };
}

const reducer = createReducer({
  [_fetchCart.succeeded]: (state, cart) => receiveCart(state, cart),
  [_fetchCustomerCart.succeeded]: (state, cart) => receiveCart(state, cart),
  [_updateLineItemCount.succeeded]: (state, cart) => receiveCart(state, cart),
  [_updateShippingMethod.succeeded]: (state, order) => receiveCart(state, order),
  [checkoutRequest]: (state) => {
    return { ...state, isCheckingOut: true };
  },
  [checkoutSuccess]: (state) => {
    return { ...state, isCheckingOut: false };
  },
  [checkoutFailed]: (state, err) => {
    console.error(err);
    return { ...state, isCheckingOut: false };
  },
}, initialState);

export default reducer;
