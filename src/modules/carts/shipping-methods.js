/* @flow */

import { createAction, createReducer } from 'redux-act';
import Api from 'lib/api';
import createAsyncActions from 'modules/async-utils';

const initialState = {
  list: [],
};

const _getShippingMethods = createAsyncActions(
  'getShippingMethods',
  (refNum: string) => {
    return Api.get(`/shipping-methods/${refNum}`);
  }
);

const _updateShippingMethod = createAsyncActions(
  'updateShippingMethod',
  (refNum: string, shippingMethodId: number) => {
    const payload = {
      shippingMethodId
    };
    return Api.patch(`orders/${refNum}/shipping-method`, payload);
  }
);

export function fetchShippingMethods(refNum: string) {
  return dispatch => dispatch(_getShippingMethods.perform(refNum));
}

export function updateShippingMethod(refNum: string, shippingMethodId: number) {
  return dispatch => dispatch(_updateShippingMethod.perform(refNum, shippingMethodId));
}

const reducer = createReducer({
  [_getShippingMethods.succeeded]: (state, list) => {
    return { ...state, list };
  },
  [_updateShippingMethod.succeeded]: (state, order) => {
    return state;
  },
}, initialState);

export default reducer;
