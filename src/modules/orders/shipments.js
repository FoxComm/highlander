/* @flow */
// libs
import Api, { request } from '../../lib/api';

// helpers
import createStore from '../../lib/store-creator';

// types
import type { TShipment, TUnshippedLineItem } from 'paragons/shipment';

type ShipmentsState = {
  shipments: Array<TShipment>,
  unshippedItems: Array<TUnshippedLineItem>,
};

const initialState: ShipmentsState = {
  shipments: [],
  unshippedItems: [],
};

const reducers = {
  setData: function (state: Object, data: ShipmentsState): Object {
    return {
      ...state,
      ...data,
    };
  },
};

function fetchShipments(actions: Object, state: Object, referenceNumber: string): Function {
  return dispatch =>
    Api.get(`/inventory/shipments/${referenceNumber}`)
      .then(data => dispatch(actions.setData(data)));
}

const { actions, reducer } = createStore({
  path: 'orders.shipments',
  asyncActions: {
    fetchShipments,
  },
  reducers,
  initialState,
});

export {
  actions,
  reducer as default
};
