/* @flow */

// libs
import { combineReducers } from 'redux';

// data
import list from './list';
import bulk from './bulk';

const catalogReducer = combineReducers({
  list,
  bulk,
});

export default catalogReducer;
