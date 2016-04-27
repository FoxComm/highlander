
import _ from 'lodash';
import createStore from '../../lib/store-creator';

import { initialState, reducers } from '../bulk';

const { actions, reducer } = createStore({
  entity: 'bulk',
  scope: 'coupons',
  actions: [],
  reducers,
  initialState,
});

export {
  actions,
  reducer as default
};
