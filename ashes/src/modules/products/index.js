/**
 * @flow
 */

// libs
import { combineReducers } from 'redux';

// data
import amazon from './amazon';
import details from './details';
import list from './list';
import images from './images';
import imagesBulk from './images-bulk';


const productReducer = combineReducers({
  amazon,
  details,
  list,
  images,
  imagesBulk,
});

export default productReducer;
