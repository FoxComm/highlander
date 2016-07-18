import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Product',
    type: 'object',
    options: [
      {
        title: 'Name',
        type: 'string',
        term: 'skuTitle'
      }, {
        title: 'SKU',
        type: 'string',
        term: 'skuCode'
      }, {
        title: 'Price',
        type: 'currency',
        term: 'skuPrice'
      }, {
        title: 'Order',
        type: 'number',
        term: 'orderReferenceNumber'
      }, {
        title: 'Date/Time Order Placed',
        type: 'datetime',
        term: 'orderPlacedAt'
      }, {
        title: 'Favorite',
        type: 'exists',
        term: 'savedForLaterAt',
        suggestions: [
          { display: 'Yes', operator: 'exists' },
          { display: 'No', operator: 'missing' },
        ]
      }
    ]
  }
];

const { reducer, actions } = makeLiveSearch(
  'customers.items',
  searchTerms,
  'customer_items_view/_search',
  'customerItemsScope',
  {
    skipInitialFetch: true,
    initialState: { sortBy: '-orderPlacedAt' }
  }
);

export {
  reducer as default,
  actions
};
