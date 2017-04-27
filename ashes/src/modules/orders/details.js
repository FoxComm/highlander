/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import OrderParagon from 'paragons/order';
import { createAsyncActions } from '@foxcomm/wings';

export type State = {
  order: ?OrderParagon,
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

const _updateShipments = createAsyncActions(
  'updateShipments',
  (refNum: string) => Api.patch(`/inventory/shipments/for-order/${refNum}`, { state: 'shipped' })
);

export const fetchOrder = _getOrder.perform;
export const updateOrder = _updateOrder.perform;
export const updateShipments = _updateShipments.perform;
export const clearFetchErrors =_getOrder.clearErrors;

function orderSucceeded(state: State, payload: Object): State {
  const order: OrderParagon = new OrderParagon(payload.result || payload);

  return { ...state, order };
}

export function increaseRemorsePeriod(refNum: string) {
  return (dispatch: Function): Promise<*> =>
    Api.post(`/orders/${refNum}/increase-remorse-period`)
      .then(order => dispatch(_updateOrder.succeeded(order)));
}

const reducer = createReducer({
  [_getOrder.succeeded]: orderSucceeded,
  [_updateOrder.succeeded]: orderSucceeded,
  [_updateShipments.succeeded]: (state) => state,
}, initialState);

export default reducer;
