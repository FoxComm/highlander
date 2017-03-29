// @flow

import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import flatList from './flatList';

const taxonomyReducer = combineReducers({
  details,
  flatList, // flatList is for list of taxonomies that can be used in menu, widgets etc.
  list, // for saved searches
});

export default taxonomyReducer;
