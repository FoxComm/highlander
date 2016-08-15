/* @flow */

// libs
import _ from 'lodash';
import Api, { request } from '../../lib/api';

// helpers
import type { Store } from '../../lib/store-creator';
import createStore from '../../lib/store-creator';

// types
import type { Carrier } from 'paragons/shipment';


type CarriersState = {
  list: Array<Carrier>,
};

const initialState: CarriersState = {
  list: []
};

const reducers = {
  setList: function (state: Object, list: Array<Carrier>): Object {
    return {
      ...state,
      list,
    };
  },
};

function load(actions: Object): Function {
  return dispatch =>
    Api.get(`/inventory/carriers`)
      .then(data => dispatch(actions.setList(data)));
}

const { actions, reducer } = createStore({
  path: 'orders.carriers',
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
