/**
 * @flow
 */

// libs
import { combineReducers } from 'redux';

// data
import amazon from './amazon';

const channelsReducer = combineReducers({
  amazon,
});

export default channelsReducer;
