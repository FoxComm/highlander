import { combineReducers } from 'redux';

import list from './list';
import details from './details';
import templates from './templates';

const reducer = combineReducers({
  list,
  details,
  templates,
});

export default reducer;
