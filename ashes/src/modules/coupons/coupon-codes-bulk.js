
import createStore from '../../lib/store-creator';

import { reducers } from '../bulk';

const { actions, reducer } = createStore({
  //TODO is this store needed?
  path: 'couponCodes.bulk',
  actions: [],
  reducers,
});

export {
  actions,
  reducer as default
};
