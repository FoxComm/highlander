
import makeLiveSearch from '../live-search';

const searchTerms = [];

const { reducer, actions } = makeLiveSearch(
  'customers-storeCreditTransactions',
  searchTerms,
  'store_credit_transactions_view/_search',
  'customerStoreCreditTransactionsScope'
);

export {
  reducer as default,
  actions
};
