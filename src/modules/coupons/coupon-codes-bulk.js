
import createStore from '../../lib/store-creator';

import { initialState, reducers } from '../bulk';

const { actions, reducer } = createStore({
  entity: 'bulk',
  scope: 'couponCodes',
  actions: [],
  reducers,
});

export {
  actions,
  reducer as default
};
