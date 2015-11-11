import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { orderSuccess } from './details.js'

export const orderShippingMethodRequest = createAction('ORDER_SHIPPING_METHOD_REQUEST');
export const orderShippingMethodRequestSuccess = createAction('ORDER_SHIPPING_METHOD_REQUEST_SUCCESS');
export const orderShippingMethodRequestFailed = createAction('ORDER_SHIPPING_METHOD_REQUEST_FAILED');
export const orderShippingMethodStartEdit = createAction('ORDER_SHIPPING_METHOD_START_EDIT');
export const orderShippingMethodCancelEdit = createAction('ORDER_SHIPPING_METHOD_CANCEL_EDIT');
export const orderShippingMethodStartEditPrice = createAction('ORDER_SHIPPING_METHOD_START_EDIT_PRICE');
export const orderShippingMethodCancelEditPrice = createAction('ORDER_SHIPPING_METHOD_CANCEL_EDIT_PRICE');
export const orderShippingMethodUpdate = createAction('ORDER_SHIPPING_METHOD_UPDATE');
export const orderShippingMethodUpdateSuccess = createAction('ORDER_SHIPPING_METHOD_UPDATE_SUCCESS');
export const orderShippingMethodUpdateFailed = createAction('ORDER_SHIPPING_METHOD_UPDATE_FAILED');

export function fetchShippingMethods(order) {
  return dispatch => {
    dispatch(orderShippingMethodRequest());
    return Api.get(`/shipping-methods/${order.referenceNumber}`)
      .then(methods => {
        dispatch(orderShippingMethodRequestSuccess(methods));
        dispatch(orderShippingMethodStartEdit());
      })
      .catch(err => dispatch(orderShippingMethodRequestFailed(err)));
  };
}

export function updateShippingMethod(order, shippingMethod) {
  return dispatch => {
    dispatch(orderShippingMethodUpdate());
    const payload = { shippingMethodId: shippingMethod.id };
    return Api.patch(`/orders/${order.referenceNumber}/shipping-method`, payload)
      .then(order => {
        dispatch(orderShippingMethodUpdateSuccess());
        dispatch(orderSuccess(order));
      })
      .catch(error => dispatch(orderShippingMethodUpdateFailed(err)));
  };
}

const initialState = {
  isEditing: false,
  isEditingPrice: false,
  isFetching: false,
  isUpdating: false,
  availableMethods: []
};

const reducer = createReducer({
  [orderShippingMethodRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [orderShippingMethodRequestSuccess]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      availableMethods: payload
    };
  },
  [orderShippingMethodRequestFailed]: (state, err) => {
    console.error(err);
    return {
      ...state,
      isFetching: false
    };
  },
  [orderShippingMethodStartEdit]: (state) => {
    return {
      ...state,
      isEditing: true,
      isEditingPrice: false
    };
  },
  [orderShippingMethodCancelEdit]: (state) => {
    return {
      ...state,
      isEditing: false,
      isEditingPrice: false
    };
  },
  [orderShippingMethodStartEditPrice]: (state) => {
    return {
      ...state,
      isEditingPrice: true
    };
  },
  [orderShippingMethodCancelEditPrice]: (state) => {
    return {
      ...state,
      isEditingPrice: false
    };
  },
  [orderShippingMethodUpdate]: (state) => {
    return {
      ...state,
      isUpdating: true
    };
  },
  [orderShippingMethodUpdateSuccess]: (state) => {
    return {
      ...state,
      isUpdating: false
    };
  },
  [orderShippingMethodUpdateFailed]: (state, err) => {
    console.log(err);
    return {
      ...store,
      isUpdating: false
    };
  }
}, initialState);

export default reducer;
