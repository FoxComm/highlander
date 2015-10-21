'use strict';

import { combineReducers } from 'redux';
import details from './details';
import list from './list';

const orderReducer = combineReducers({
  details,
  list
});

export default orderReducer;
