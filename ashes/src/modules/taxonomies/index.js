// @flow

import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import flatList from './flatList';
import bulk from './bulk';

const taxonomyReducer = combineReducers({
  details,
  bulk,
  flatList, // flatList is for list of taxonomies that can be used in menu, widgets etc.
  list, // for saved searches
});

export default taxonomyReducer;
