/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import OrderParagon from 'paragons/order';
import createAsyncActions from 'modules/async-utils';

const initialState = {
  order: null,
};

const _getOrder = createAsyncActions(
  'getOrder',
  (refNum: string) => Api.get(`/orders/${refNum}`)
);

const _updateOrder = createAsyncActions(
  'updateOrder',
  (id: number, data: Object) => Api.patch(`/orders/${id}`, data)
);

export function fetchOrder(refNum: string) {
  return dispatch => dispatch(_getOrder.perform(refNum));
}

export function updateOrder(id: number, payload: Object) {
  return dispatch => dispatch(_updateOrder.perform(id, payload));
}

function orderSucceeded(state: Object, payload: Object) {
  const order = payload.result || payload;
  return { ...state, order: new OrderParagon(order) };
}

const reducer = createReducer({
  [_getOrder.succeeded]: (state, payload) => orderSucceeded(state, payload),
  [_updateOrder.succeeded]: (state, payload) => orderSucceeded(state, payload),
}, initialState);

export default reducer;
