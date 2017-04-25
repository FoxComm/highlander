/**
 * @flow
 */

// libs
import { combineReducers } from 'redux';

// data
import details from './details';
import list from './list';
import images from './images';
import imagesBulk from './images-bulk';
import suggest from './suggest';


const productReducer = combineReducers({
  details,
  list,
  images,
  imagesBulk,
  suggest,
});

export default productReducer;
