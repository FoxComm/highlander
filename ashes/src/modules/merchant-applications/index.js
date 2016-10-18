import { combineReducers } from 'redux';
import details from './details';
import list from './list';

const applicationReducer = combineReducers({
  details,
  list,
});

export default applicationReducer;
