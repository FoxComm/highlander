import { combineReducers } from 'redux';
import details from './details';
import bulk from './bulk';
import watchers from './watchers';
import list from './list';
import newOrder from './new-order';
import shipments from './shipments';

const orderReducer = combineReducers({
  details,
  bulk,
  watchers,
  list,
  newOrder,
  shipments,
});

export default orderReducer;
