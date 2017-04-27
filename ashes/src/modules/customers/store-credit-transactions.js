
import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Adjustment',
    type: 'object',
    options: [
      {
        title: 'ID',
        type: 'string',
        term: 'id'
      },
      {
        title: 'Created At',
        type: 'date',
        term: 'createdAt'
      },
      {
        title: 'Debit',
        type: 'currency',
        term: 'debit'
      },
      {
        title: 'Available Balance',
        type: 'currency',
        term: 'availableBalance'
      },
    ]
  },
  {
    title: 'Store Credit',
    type: 'object',
    options: [
      {
        title: 'ID',
        type: 'string',
        term: 'storeCreditId'
      },
      {
        title: 'Origin Type',
        type: 'enum',
        term: 'originType',
        suggestions: [
          { display: 'Appeasement', value: 'csrAppeasement' },
          { display: 'Customer Purchase', value: 'customerPurchase' },
          { display: 'From Store Credit', value: 'fromStoreCredit' },
          { display: 'RMA Process', value: 'rmaProcess' },
        ]
      },
      {
        title: 'Created Date',
        type: 'date',
        term: 'storeCreditCreatedAt'
      },
    ]
  },
  {
    title: 'Order',
    type: 'object',
    options: [
      {
        title: 'Reference Number',
        type: 'identifier',
        term: 'orderReferenceNumber'
      },
      {
        title: 'Created At',
        type: 'date',
        term: 'orderCreatedAt'
      },
      {
        title: 'Payment Created At',
        type: 'date',
        term: 'orderPaymentCreatedAt'
      },
    ]
  }
];

const { reducer, actions } = makeLiveSearch(
  'customers.storeCreditTransactions',
  searchTerms,
  'store_credit_transactions_search_view/_search',
  'customerStoreCreditTransactionsScope', {
    initialState: { sortBy: '-createdAt' },
    skipInitialFetch: true
  }
);

export {
  reducer as default,
  actions
};
