/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcommerce/wings';

const initialState = {
  list: [],
};

const _getShippingMethods = createAsyncActions(
  'getShippingMethods',
  (refNum: string) => {
    return Api.get(`/shipping-methods/${refNum}`);
  }
);

export function fetchShippingMethods(refNum: string): Function {
  return dispatch => dispatch(_getShippingMethods.perform(refNum));
}

const reducer = createReducer({
  [_getShippingMethods.succeeded]: (state, list) => {
    return { ...state, list };
  },
}, initialState);

export default reducer;
