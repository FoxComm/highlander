
import _ from 'lodash';
import Api from 'lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
// import { orderLineItemsFetchSuccess } from './line-items';
import OrderParagon from 'paragons/order';

export const cartRequest = createAction('CART_REQUEST');
export const customerCartRequest = createAction('CUSTOMER_CART_REQUEST');
export const cartSuccess = createAction('CART_SUCCESS');
export const cartFailed = createAction('CART_FAILED', (err, source) => [err, source]);
// it's for optimistic update
// TODO: research for general approach ?
export const optimisticSetShippingMethod = createAction('ORDER_LUCKY_SET_SHIPPING_METHOD');
export const optimisticRevertShippingMethod = createAction('ORDER_REVERT_SHIPPING_METHOD');

const checkoutRequest = createAction('ORDER_CHECKOUT_REQUEST');
const checkoutSuccess = createAction('ORDER_CHECKOUT_SUCCESS');
const checkoutFailed = createAction('ORDER_CHECKOUT_FAILURE');


function baseFetchCart(url, actionBefore) {
  return dispatch => {
    dispatch(actionBefore);
    return Api.get(url)
      .then(order => {
          dispatch(cartSuccess(order));
          // dispatch(orderLineItemsFetchSuccess(order));
        },
        err => dispatch(cartFailed(err, baseFetchCart)));
  };
}

export function fetchCart(refNum) {
  return baseFetchCart(`/carts/${refNum}`, cartRequest(refNum));
}

export function fetchCustomerCart(customerId) {
  return baseFetchCart(`/customers/${customerId}/cart`, customerCartRequest(customerId));
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

export function updateOrder(id, data) {
  return dispatch => {
    dispatch(cartRequest(id));
    Api.patch(`/orders/${id}`, data)
      .then(
        order => {
          dispatch(cartSuccess(order));
          // dispatch(orderLineItemsFetchSuccess(order));
        },
        err => dispatch(cartFailed(id, err, updateOrder))
      );
  };
}

export function increaseRemorsePeriod(refNum) {
  return dispatch => {
    dispatch(cartRequest(refNum));
    return Api.post(`/orders/${refNum}/increase-remorse-period`)
      .then(
        order => {
          dispatch(cartSuccess(order));
          // dispatch(orderLineItemsFetchSuccess(order));
        },
        err => dispatch(cartFailed(err, fetchOrder))
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
  isFetching: false,
  failed: null,
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

const reducer = createReducer({
  [cartRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [customerCartRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [cartSuccess]: (state, payload) => {
    const order = _.get(payload, 'result', payload);
    const skus = _.get(order, 'lineItems.skus', []);
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
      isFetching: false,
      failed: null,
      cart: new OrderParagon(order),
      validations: {
        errors: errors,
        warnings: warnings,
        ...status
      }
    };
  },
  [optimisticSetShippingMethod]: (state, shippingMethod) => {
    const newOrder = assoc(state.cart,
      '_shippingMethod', state.cart._shippingMethod || state.cart.shippingMethod,
      'shippingMethod', shippingMethod
    );

    return {
      ...state,
      cart: new OrderParagon(newOrder)
    };
  },
  [optimisticRevertShippingMethod]: state => {
    const newOrder = assoc(state.cart,
      'shippingMethod', state.cart._shippingMethod,
      '_shippingMethod', null
    );

    return {
      ...state,
      cart: new OrderParagon(newOrder)
    };
  },
  [cartFailed]: (state, [err, source]) => {
    if (source === baseFetchCart) {
      console.error(err);

      return {
        ...state,
        failed: true,
        isFetching: false
      };
    }

    return state;
  },
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
