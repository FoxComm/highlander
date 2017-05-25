import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import images from './images';
import suggest from './suggest';
import bulk from './bulk';

const skuReducer = combineReducers({
  details,
  bulk,
  list,
  images,
  suggest,
});

export default skuReducer;
