// @flow

import { combineReducers } from 'redux';

import list from './list';
import details from './details';
import bulk from './bulk';

const taxonReducer = combineReducers({
  list,
  bulk,
  details,
});

export default taxonReducer;
