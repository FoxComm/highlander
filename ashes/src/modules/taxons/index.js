// @flow

import { combineReducers } from 'redux';
import list from './list';
import details from './details';

const taxonReducer = combineReducers({
  list,
  details
});

export default taxonReducer;
