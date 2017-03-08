import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import images from './images';
import suggest from './suggest';

const pvReducer = combineReducers({
  details,
  list,
  images,
  suggest,
});

export default pvReducer;
