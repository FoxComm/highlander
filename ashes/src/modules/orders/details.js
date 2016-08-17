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

export const fetchOrder = _getOrder.perform;
export const updateOrder = _updateOrder.perform;
export const clearFetchErrors =_getOrder.clearErrors;

function orderSucceeded(state: State, payload: Object): State {
  const order = payload.result || payload;
  return { ...state, order: new OrderParagon(order) };
}

const reducer = createReducer({
  [_getOrder.succeeded]: orderSucceeded,
  [_updateOrder.succeeded]: orderSucceeded,
}, initialState);

export default reducer;
