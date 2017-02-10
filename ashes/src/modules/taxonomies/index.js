// @flow

import { combineReducers } from 'redux';
import list from './list';

const taxonomyReducer = combineReducers({
  list,
});

export default taxonomyReducer;
