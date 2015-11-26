import { combineReducers } from 'redux';
import customers from './list';
import adding from './new';

import _details from './details';
import addressesDetails from './addresses-details';
import addresses from './addresses';
import contacts from './contacts';
import creditCards from './credit-cards';
import storeCredits from './store-credits';
import reduceReducers from 'reduce-reducers';

const details = reduceReducers(_details, contacts);

const customerReducer = combineReducers({
  customers,
  adding,
  details,
  addressesDetails,
  addresses,
  creditCards,
  storeCredits
});

export default customerReducer;
