import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Store Credit',
    type: 'object',
    options: [
      {
        title: 'ID',
        type: 'string',
        term: 'id'
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
        title: 'State',
        type: 'enum',
        term: 'state',
        suggestions: [
          { display: 'Cart', value: 'cart' },
          { display: 'Active', value: 'active' },
          { display: 'Fully Redeemed', value: 'fullyRedeemed' },
          { display: 'Canceled', value: 'canceled' },
        ]
      },
      {
        title: 'Original Balance',
        type: 'currency',
        term: 'originalBalance'
      },
      {
        title: 'Current Balance',
        type: 'currency',
        term: 'currentBalance'
      },
      {
        title: 'Available Balance',
        type: 'currency',
        term: 'availableBalance'
      },
      {
        title: 'Canceled Amount',
        type: 'currency',
        term: 'canceledAmount'
      },
      {
        title: 'Created Date',
        type: 'date',
        term: 'createdAt'
      },
    ]
  },
];

const { reducer, actions } = makeLiveSearch(
  'customers.storeCredits',
  searchTerms,
  'store_credits_search_view',
  'customerStoreCreditsScope', {
    skipInitialFetch: true
  }
);

export {
  reducer as default,
  actions
};
