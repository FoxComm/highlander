/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import createAsyncActions from 'modules/async-utils';

import type { ShippingMethod } from 'paragons/merchant-application';

type State = {
  shippingMethods: Array<ShippingMethod>,
};

const initialState = {
  shippingMethods: [],
};

const _fetchShippingMethods = createAsyncActions(
  'fetchShippingMethods',
  () => Api.get('/shipping-methods')
);

export const fetchShippingMethods = _fetchShippingMethods.perform;

const reducer = createReducer({
  [_fetchShippingMethods.succeeded]: (state: State, payload: Object): State => {
	  const shippingMethods = payload.result || payload;
	  return { ...state, shippingMethods };
	},
}, initialState);

export default reducer;
