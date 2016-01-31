import { combineReducers } from 'redux';
import list from './list';
import adding from './new';

import _details from './details';
import addressesDetails from './addresses-details';
import addresses from './addresses';
import contacts from './contacts';
import transactions from './transactions';
import creditCards from './credit-cards';
import storeCredits from './store-credits';
import storeCreditTransactions from './store-credit-transactions';
import newStoreCredit from './new-store-credit';
import storeCreditTotals from './store-credit-totals';
import reduceReducers from 'reduce-reducers';

const details = reduceReducers(_details, contacts);

const customerReducer = combineReducers({
  list,
  adding,
  details,
  transactions,
  addressesDetails,
  addresses,
  creditCards,
  storeCreditTotals,
  storeCredits,
  storeCreditTransactions,
  newStoreCredit
});

export default customerReducer;
