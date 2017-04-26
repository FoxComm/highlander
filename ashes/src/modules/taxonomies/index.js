// @flow

import { combineReducers } from 'redux';
import list from './list';
import suggest from './suggest';

const taxonomyReducer = combineReducers({
  list,
  suggest,
});

export default taxonomyReducer;
