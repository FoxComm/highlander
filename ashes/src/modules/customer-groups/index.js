import { combineReducers } from 'redux';

import list from './list';
import details from './details';
import templates from './templates';
import suggest from './suggest';
import bulk from './bulk';

const reducer = combineReducers({
  list,
  bulk,
  details,
  templates,
  suggest,
});

export default reducer;
