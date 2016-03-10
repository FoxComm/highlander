// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';

const initialState = {
  name: null,
  query: {},
  customersCount: 0,
};

const reducers = {
  setName: (state, name) => {
    return assoc(state, 'name', name);
  },
  setQuery: (state, query) => {
    return assoc(state, 'query', query);
  },
};

const { actions, reducer } = createStore({
  entity: 'customer-groups',
  actions: {},
  reducers,
  initialState,
});

export {
  actions,
  reducer as default
};
