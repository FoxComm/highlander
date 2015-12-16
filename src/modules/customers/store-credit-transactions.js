
import _ from 'lodash';
import makePagination from '../pagination/structured-store';

const dataPath = storeCreditId => [storeCreditId];

const { makeActions, makeReducer } = makePagination('STORECREDIT_TRANSACTIONS', dataPath);

function storeCreditTransactionsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit/transactions`;
}

const { fetch } = makeActions(storeCreditTransactionsUrl);
const reducer = makeReducer();

export {
  reducer as default,
  fetch as fetchStoreCreditTransactions
};
