// @flow

import { combineReducers } from 'redux';
import list from './list';

const taxonReducer = combineReducers({
  list,
});

export default taxonReducer;
