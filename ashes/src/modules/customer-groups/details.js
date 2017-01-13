import { combineReducers } from 'redux';

import group from './group';
import customers from './customers-list';

const reducer = combineReducers({
  group,
  customers,
});

export default reducer;
