/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import OrderParagon from 'paragons/order';
import createAsyncActions from 'modules/async-utils';

import type { Order } from 'paragons/order';

type State = {
  order: ?Order,
};

const initialState: State = {
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

export function fetchOrder(refNum: string): Function {
  return dispatch => dispatch(_getOrder.perform(refNum));
}

export function updateOrder(id: number, payload: Object): Function {
  return dispatch => dispatch(_updateOrder.perform(id, payload));
}

function orderSucceeded(state: State, payload: Object): State {
  const order = payload.result || payload;
  return { ...state, order: new OrderParagon(order) };
}

const reducer = createReducer({
  [_getOrder.succeeded]: orderSucceeded,
  [_updateOrder.succeeded]: orderSucceeded,
}, initialState);

export default reducer;
