import { combineReducers } from 'redux';
import details from './details';
import list from './list';
import images from './images';

const skuReducer = combineReducers({
  details,
  list,
  images,
});

export default skuReducer;
