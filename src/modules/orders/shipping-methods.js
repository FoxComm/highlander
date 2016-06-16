import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { orderSuccess, optimisticSetShippingMethod, optimisticRevertShippingMethod } from './details.js';

const _createAction = (description, ...args) => {
  return createAction('ORDER_SHIPPING_METHOD_' + description, ...args);
};

export const orderShippingMethodRequest = _createAction('REQUEST');
export const orderShippingMethodRequestSuccess = _createAction('REQUEST_SUCCESS');
export const orderShippingMethodRequestFailed = _createAction('REQUEST_FAILED');
export const orderShippingMethodStartEdit = _createAction('START_EDIT');
export const orderShippingMethodCancelEdit = _createAction('CANCEL_EDIT');
export const orderShippingMethodStartEditPrice = _createAction('START_EDIT_PRICE');
export const orderShippingMethodCancelEditPrice = _createAction('CANCEL_EDIT_PRICE');
export const orderShippingMethodUpdate = _createAction('UPDATE');
export const orderShippingMethodUpdateSuccess = _createAction('UPDATE_SUCCESS');
export const orderShippingMethodUpdateFailed = _createAction('UPDATE_FAILED');

export function fetchShippingMethods(order) {
  return dispatch => {
    dispatch(orderShippingMethodRequest());
    return Api.get(`/shipping-methods/${order.referenceNumber}`)
      .then(
        methods => {
          dispatch(orderShippingMethodRequestSuccess(methods));
          dispatch(orderShippingMethodStartEdit());
        },
        err => dispatch(orderShippingMethodRequestFailed(err))
      );
  };
}


// assume we have one scope for updating shipping method
let updateShippingMethodRequest;

export function updateShippingMethod(order, shippingMethod) {
  return dispatch => {
    if (updateShippingMethodRequest) {
      updateShippingMethodRequest.abort();
    }

    dispatch(orderShippingMethodUpdate());
    dispatch(optimisticSetShippingMethod(shippingMethod));
    const payload = { shippingMethodId: shippingMethod.id };
    updateShippingMethodRequest = Api.patch(`/orders/${order.referenceNumber}/shipping-method`, payload)
      .then(
        order => {
          dispatch(orderShippingMethodUpdateSuccess());
          dispatch(orderSuccess(order));
        },
        err => {
          dispatch(optimisticRevertShippingMethod());
          dispatch(orderShippingMethodUpdateFailed(err));
        }
      );

    return updateShippingMethodRequest;
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
    console.error(err);
    return {
      ...state,
      isUpdating: false
    };
  }
}, initialState);

export default reducer;
