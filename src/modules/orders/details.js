
import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';
import { get, assoc } from 'sprout-data';
import { orderLineItemsFetchSuccess } from './line-items';
import OrderParagon from '../../paragons/order';

export const orderRequest = createAction('ORDER_REQUEST');
export const cartRequest = createAction('CART_REQUEST');
export const orderSuccess = createAction('ORDER_SUCCESS');
export const orderFailed = createAction('ORDER_FAILED', (err, source) => [err, source]);

const checkoutRequest = createAction('ORDER_CHECKOUT_REQUEST');
const checkoutSuccess = createAction('ORDER_CHECKOUT_SUCCESS');
const checkoutFailure = createAction('ORDER_CHECKOUT_FAILURE');


function baseFetchOrder(url, actionBefore) {
  return dispatch => {
    dispatch(actionBefore);
    return Api.get(url)
      .then(order => {
          dispatch(orderSuccess(order));
          dispatch(orderLineItemsFetchSuccess(order));
        },
        err => dispatch(orderFailed(err, baseFetchOrder)));
  };
}

export function fetchOrder(refNum) {
  return baseFetchOrder(`/orders/${refNum}`, orderRequest(refNum));
}

export function fetchCustomerCart(customerId) {
  return baseFetchOrder(`/customers/${customerId}/cart`, cartRequest(customerId));
}

export function checkout(refNum) {
  return dispatch => {
    dispatch(checkoutRequest());
    return Api.post(`/orders/${refNum}/checkout`)
      .then(
        order => {
          dispatch(orderSuccess(order));
          dispatch(checkoutSuccess());
        },
        err => dispatch(checkoutFailed(err))
      );
  };
}

export function updateOrder(id, data) {
  return dispatch => {
    dispatch(orderRequest(id));
    Api.patch(`/orders/${id}`, data)
      .then(
        order => {
          dispatch(orderSuccess(order));
          dispatch(orderLineItemsFetchSuccess(order));
        },
        err => dispatch(orderFailed(id, err, updateOrder))
      );
  };
}

export function increaseRemorsePeriod(refNum) {
  return dispatch => {
    dispatch(orderRequest(refNum));
    return Api.post(`/orders/${refNum}/increase-remorse-period`)
      .then(
        order => {
          dispatch(orderSuccess(order));
          dispatch(orderLineItemsFetchSuccess(order));
        },
        err => dispatch(orderFailed(err, fetchOrder))
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
  currentOrder: {},
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
  [orderRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [cartRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [orderSuccess]: (state, payload) => {
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
      currentOrder: new OrderParagon(order),
      validations: {
        errors: errors,
        warnings: warnings,
        ...status
      }
    };
  },
  [orderFailed]: (state, [err, source]) => {
    if (source === baseFetchOrder) {
      console.error(err);

      return {
        ...state,
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
  [checkoutRequest]: (state, err) => {
    console.error(err);
    return { ...state, isCheckingOut: false };
  },
}, initialState);

export default reducer;
