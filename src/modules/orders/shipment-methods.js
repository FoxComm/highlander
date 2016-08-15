/* @flow */

// libs
import _ from 'lodash';
import Api, { request } from '../../lib/api';
import shipmentMethods from './mocks/shipment-methods.json';

// helpers
import type { Store } from '../../lib/store-creator';
import createStore from '../../lib/store-creator';

// types
import type { ShippingMethod } from 'paragons/shipment';


type ShippingMethodsState = {
  list: Array<ShippingMethod>,
};

const initialState: ShippingMethodsState = {
  list: []
};

const reducers = {
  setList: function (state: Object, list: Array<ShippingMethod>): Object {
    return {
      ...state,
      list,
    };
  },
};

function load(actions: Object): Function {
  return dispatch =>
    Api.get(`/inventory/shipping-methods`)
      .then(data => dispatch(actions.setList(data)));
}

const { actions, reducer } = createStore({
  path: 'orders.shipmentMethods',
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
