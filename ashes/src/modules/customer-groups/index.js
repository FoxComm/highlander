import { combineReducers } from 'redux';

import list from './list';
import details from './details';
import templates from './templates';
import suggest from './suggest';

const reducer = combineReducers({
  list,
  details,
  templates,
  suggest,
});

export default reducer;
