/* @flow */

// libs
import { combineReducers } from 'redux';

// data
import details from './details';
import list from './list';
import bulk from './bulk';

const catalogReducer = combineReducers({
  details,
  list,
  bulk,
});

export default catalogReducer;
