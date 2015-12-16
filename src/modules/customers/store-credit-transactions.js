
import _ from 'lodash';
import makePagination from '../pagination/structuredStore';

const dataNamespace = ['customers', 'storeCreditTransactions'];
const dataPath = storeCreditId => [storeCreditId];

const { makeActions, makeReducer } = makePagination(dataNamespace, dataPath);

function storeCreditTransactionsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit/transactions`;
}

const { fetch } = makeActions(storeCreditTransactionsUrl);
const reducer = makeReducer();

export {
  reducer as default,
  fetch as fetchStoreCreditTransactions
};
