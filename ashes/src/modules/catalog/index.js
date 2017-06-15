/* @flow */

// libs
import { combineReducers } from 'redux';

// data
import details from './details';
import list from './list';
import bulk from './bulk';
import products from './products-list';

const catalogReducer = combineReducers({
  details,
  list,
  bulk,
  products,
});

export default catalogReducer;
