/* @flow */

// libs
import Api, { request } from '../../lib/api';

// helpers
import createStore from '../../lib/store-creator';

// types
import type { ShippingMethod } from 'paragons/shipment';


type ShippingMethodsState = {
  list: Array<ShippingMethod>,
};

const initialState: ShippingMethodsState = {
  list: [],
};

const reducers = {
  setList: function (state: Object, list: Array<ShippingMethod>): Object {
    return {
      ...state,
      list,
    };
  },
};

function fetchShippingMethods(actions: Object): Function {
  return dispatch =>
    Api.get(`/inventory/shipping-methods`)
      .then(data => dispatch(actions.setList(data)));
}

const { actions, reducer } = createStore({
  path: 'orders.shipmentMethods',
  asyncActions: {
    fetchShippingMethods,
  },
  reducers,
  initialState,
});

export {
  actions,
  reducer as default
};
