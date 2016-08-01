/* @flow */
import _ from 'lodash';
import Api from 'lib/api';
import { createReducer } from 'redux-act';
import { transitionTo } from 'browserHistory';
import OrderParagon from 'paragons/order';

import createAsyncActions from 'modules/async-utils';

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

export function fetchCart(refNum: string): Function {
  return dispatch => dispatch(_fetchCart.perform(refNum));
}

export function fetchCustomerCart(customerId: number): Function {
  return dispatch => dispatch(_fetchCustomerCart.perform(customerId));
}

////////////////////////////////////////////////////////////////////////////////
// Line Item Actions

const _updateLineItemCount = createAsyncActions(
  'updateLineItemCount',
  (refNum: string, payload: Object) => Api.post(`/orders/${refNum}/line-items`, payload)
);

export function updateLineItemCount(refNum: string, sku: string, quantity: number): Function {
  const payload = [{ sku, quantity }];
  return dispatch => dispatch(_updateLineItemCount.perform(refNum, payload));
}

export function deleteLineItem(refNum: string, sku: string): Function {
  const payload = [{ sku, quantity: 0 }];
  return dispatch => dispatch(_updateLineItemCount.perform(refNum, payload));
}

////////////////////////////////////////////////////////////////////////////////
// Shipping Address Actions

const _chooseShippingAddress = createAsyncActions(
  'chooseShippingAddress',
  (refNum: string, addressId: number) => Api.patch(`/orders/${refNum}/shipping-address/${addressId}`)
);

const _createShippingAddress = createAsyncActions(
  'createShippingAddress',
  (refNum: string, payload: Object) => Api.post(`/orders/${refNum}/shipping-address`, payload)
);

const _updateShippingAddress = createAsyncActions(
  'updateShippingAddress',
  (refNum: string, payload: Object) => Api.patch(`/orders/${refNum}/shipping-address`, payload)
);

const _deleteShippingAddress = createAsyncActions(
  'deleteShippingAddress',
  (refNum: string) => Api.delete(`/orders/${refNum}/shipping-address`)
);

export function chooseShippingAddress(refNum: string, addressId: number): Function {
  return dispatch => dispatch(_chooseShippingAddress.perform(refNum, addressId));
}

export function createShippingAddress(refNum: string, payload: Object): Function {
  return dispatch => dispatch(_createShippingAddress.perform(refNum, payload));
}

export function updateShippingAddress(refNum: string, payload: Object): Function {
  return dispatch => dispatch(_updateShippingAddress.perform(refNum, payload));
}

export function deleteShippingAddress(refNum: string): Function {
  return dispatch => dispatch(_deleteShippingAddress.perform(refNum));
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

export function updateShippingMethod(refNum: string, shippingMethodId: number): Function {
  return dispatch => dispatch(_updateShippingMethod.perform(refNum, shippingMethodId));
}

////////////////////////////////////////////////////////////////////////////////
// Payment Method Actions

const _selectCreditCard = createAsyncActions(
  'selectCreditCard',
  (refNum: string, creditCardId: number) => {
    const payload = { creditCardId };
    return Api.post(`${paymentsBasePath(refNum)}/credit-cards`, payload);
  }
);

const _setStoreCreditPayment = createAsyncActions(
  'setStoreCreditPayment',
  (refNum: string, amount: number) => {
    const payload = { amount };
    return Api.post(`${paymentsBasePath(refNum)}/store-credit`, payload);
  }
);

const _addGiftCardPayment = createAsyncActions(
  'addGiftCardPayment',
  (refNum: string, code: string, amount: number) => {
    const payload = { code, amount };
    return Api.post(`${paymentsBasePath(refNum)}/gift-cards`, payload);
  }
);

const _editGiftCardPayment = createAsyncActions(
  'editGiftCardPayment',
  (refNum: string, code: string, amount: number) => {
    const payload = { code, amount };
    return Api.patch(`${paymentsBasePath(refNum)}/gift-cards`, payload);
  }
);

const _deleteCreditCardPayment = createAsyncActions(
  'deleteCreditCardPayment',
  (refNum: string) => Api.delete(`${paymentsBasePath(refNum)}/credit-card`)
);

const _deleteGiftCardPayment = createAsyncActions(
  'deleteGiftCardPayment',
  (refNum: string, code: string) => {
    return Api.delete(`${paymentsBasePath(refNum)}/gift-cards/${code}`);
  }
);

const _deleteStoreCreditPayment = createAsyncActions(
  'deleteStoreCreditPayment',
  (refNum: string) => Api.delete(`${paymentsBasePath(refNum)}/store-credit`)
);

export function selectCreditCard(refNum: string, creditCardId: number): Function {
  return dispatch => dispatch(_selectCreditCard.perform(refNum, creditCardId));
}

export function setStoreCreditPayment(refNum: string, amount: number): Function {
  return dispatch => dispatch(_setStoreCreditPayment.perform(refNum, amount));
}

export function addGiftCardPayment(refNum: string, code: string, amount: number): Function {
  return dispatch => dispatch(_addGiftCardPayment.perform(refNum, code, amount));
}

export function editGiftCardPayment(refNum: string, code: string, amount: number): Function {
  return dispatch => dispatch(_editGiftCardPayment.perform(refNum, code, amount));
}

export function deleteCreditCardPayment(refNum: string): Function {
  return dispatch => dispatch(_deleteCreditCardPayment.perform(refNum));
}

export function deleteGiftCardPayment(refNum: string, code: string): Function {
  return dispatch => dispatch(_deleteGiftCardPayment.perform(refNum, code));
}

export function deleteStoreCreditPayment(refNum: string): Function {
  return dispatch => dispatch(_deleteStoreCreditPayment.perform(refNum));
}

function paymentsBasePath(refNum: string): string {
  return `/orders/${refNum}/payment-methods`;
}

////////////////////////////////////////////////////////////////////////////////
// Checkout Actions

const _checkout = createAsyncActions(
  'checkout',
  (refNum: string) => Api.post(`/orders/${refNum}/checkout`)
);

export function checkout(refNum: string): Function {
  return dispatch => dispatch(_checkout.perform(refNum))
    .then(order => transitionTo('order', { order: refNum }));
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
  [_chooseShippingAddress.succeeded]: (state, cart) => receiveCart(state, cart),
  [_createShippingAddress.succeeded]: (state, cart) => receiveCart(state, cart),
  [_updateShippingAddress.succeeded]: (state, cart) => receiveCart(state, cart),
  [_deleteShippingAddress.succeeded]: (state, cart) => receiveCart(state, cart),
  [_updateShippingMethod.succeeded]: (state, order) => receiveCart(state, order),
  [_selectCreditCard.succeeded]: (state, cart) => receiveCart(state, cart),
  [_setStoreCreditPayment.succeeded]: (state, cart) => receiveCart(state, cart),
  [_addGiftCardPayment.succeeded]: (state, cart) => receiveCart(state, cart),
  [_editGiftCardPayment.succeeded]: (state, cart) => receiveCart(state, cart),
  [_deleteCreditCardPayment.succeeded]: (state, cart) => receiveCart(state, cart),
  [_deleteGiftCardPayment.succeeded]: (state, cart) => receiveCart(state, cart),
  [_deleteStoreCreditPayment.succeeded]: (state, cart) => receiveCart(state, cart),
  [_checkout.succeeded]: (state, cart) => receiveCart(state, cart),
}, initialState);

export default reducer;
