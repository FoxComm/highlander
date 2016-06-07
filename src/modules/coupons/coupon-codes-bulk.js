
import createStore from '../../lib/store-creator';

import { initialState, reducers } from '../bulk';

const { actions, reducer } = createStore({
  //TODO is this store needed?
  path: 'bulk.couponCodes',
  actions: [],
  reducers,
});

export {
  actions,
  reducer as default
};
