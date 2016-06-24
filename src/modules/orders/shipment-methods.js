/* @flow */

// libs
import _ from 'lodash';
import Api, { request } from '../../lib/api';
import shipmentMethods from './mocks/shipment-methods.json';

// helpers
import type { Store } from '../../lib/store-creator';
import createStore from '../../lib/store-creator';

const initialState = {
  list: []
};

const reducers = {
  setData: (state: Object, list: Array<Object>): Object => ({...state, list}),
};

function load(actions: Object): Function {
  return dispatch => new Promise(resolve => {
    setTimeout(()=>{
      dispatch(actions.setData(shipmentMethods));
      resolve(shipmentMethods);
    }, 500);
  });
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
