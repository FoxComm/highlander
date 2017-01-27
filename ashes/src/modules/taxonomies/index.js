// @flow

import { combineReducers } from 'redux';
import details from './details';
import list from './list';

const taxonomyReducer = combineReducers({
  details,
  list,
});

export default taxonomyReducer;
