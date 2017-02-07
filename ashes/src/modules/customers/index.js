import { combineReducers } from 'redux';
import list from './list';

import _details from './details';
import addressesDetails from './addresses-details';
import addresses from './addresses';
import contacts from './contacts';
import transactions from './transactions';
import creditCards from './credit-cards';
import storeCreditBulk from './store-credit-bulk';
import storeCredits from './store-credits';
import storeCreditTransactions from './store-credit-transactions';
import newStoreCredit from './new-store-credit';
import storeCreditTotals from './store-credit-totals';
import storeCreditStates from './store-credit-states';
import items from './items';
import reduceReducers from 'reduce-reducers';
import suggest from './suggest';

const details = reduceReducers(_details, contacts);

const customerReducer = combineReducers({
  list,
  details,
  transactions,
  addressesDetails,
  addresses,
  creditCards,
  storeCreditBulk,
  storeCreditTotals,
  storeCredits,
  storeCreditStates,
  storeCreditTransactions,
  newStoreCredit,
  items,
  suggest,
});

export default customerReducer;
