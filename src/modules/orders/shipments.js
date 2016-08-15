/* @flow */
// libs
import _ from 'lodash';
import Api, { request } from '../../lib/api';

// helpers
import type { Store } from '../../lib/store-creator';
import createStore from '../../lib/store-creator';

// types
import type { Shipment, ShipmentLineItem, UnshippedLineItem } from 'paragons/shipment';

type ShipmentsState = {
  shipments: Array<Shipment>,
  unshippedItems: Array<UnshippedLineItem>,
};

const initialState: ShipmentsState = {
  shipments: [],
  unshippedItems: []
};

const reducers = {
  setData: function (state: Object, data: ShipmentsState): Object {
    return {
      ...state,
      ...data,
    };
  },
};

function load(actions: Object, state: Object, referenceNumber: string): Function {
  return dispatch =>
    Api.get(`/inventory/shipments/${referenceNumber}`)
      .then(shipments => dispatch(actions.setData({shipments, unshippedItems:[]})));
}

const { actions, reducer } = createStore({
  path: 'orders.shipments',
  asyncActions: {
    load
  },
  reducers,
  initialState,
});

export {
  actions,
  reducer as default
};
