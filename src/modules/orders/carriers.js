/* @flow */

// libs
import Api, { request } from '../../lib/api';

// helpers
import createStore from '../../lib/store-creator';

// types
import type { Carrier } from 'paragons/shipment';


type CarriersState = {
  list: Array<Carrier>,
};

const initialState: CarriersState = {
  list: [],
};

const reducers = {
  setList: function (state: Object, list: Array<Carrier>): Object {
    return {
      ...state,
      list,
    };
  },
};

function fetchCarriers(actions: Object): Function {
  return dispatch =>
    Api.get(`/inventory/carriers`)
      .then(data => dispatch(actions.setList(data)));
}

const { actions, reducer } = createStore({
  path: 'orders.carriers',
  asyncActions: {
    fetchCarriers,
  },
  reducers,
  initialState,
});

export {
  actions,
  reducer as default
};
