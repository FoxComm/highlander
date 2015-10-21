'use strict';

import { combineReducers } from 'redux';
import order from './order';
import orders from './orders';

const orderReducer = combineReducers({
  order,
  orders
});

export default orderReducer;
