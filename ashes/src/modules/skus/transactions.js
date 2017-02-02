import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Date/Time',
    type: 'date',
    term: 'createdAt'
  }, {
    title: 'Warehouse',
    type: 'string',
    term: 'stockLocationName'
  }, {
    title: 'Type',
    type: 'enum',
    term: 'type',
    suggestions: [
      { display: 'Sellable', value: 'Sellable' },
      { display: 'Non-sellable', value: 'Non-sellable' },
      { display: 'Backorder', value: 'Backorder' },
      { display: 'Preorder', value: 'Preorder' },
    ]
  }, {
    title: 'State',
    type: 'enum',
    term: 'status',
    suggestions: [
      { display: 'OnHand', value: 'onHand' },
      { display: 'OnHold', value: 'onHold' },
      { display: 'Reserved', value: 'reserved' },
      { display: 'Shipped', value: 'shipped' }
    ]
  }, {
    title: 'Previous',
    type: 'number',
    term: 'quantityPrevious'
  }, {
    title: 'New',
    type: 'number',
    term: 'quantityNew'
  }, {
    title: 'Change',
    type: 'number',
    term: 'quantityChange'
  }, {
    title: 'New AFS',
    type: 'number',
    term: 'afsNew'
  }
];

const { reducer, actions } = makeLiveSearch(
  'skus.transactions',
  searchTerms,
  'inventory_transactions_search_view/_search',
  'inventoryScope',
  {
    initialState: { sortBy: '-createdAt' },
    skipInitialFetch: true
  }
);

export {
  reducer as default,
  actions
};
