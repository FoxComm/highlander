import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Transaction',
    type: 'object',
    options: [
      {
        title: 'Date/Time',
        type: 'date',
        term: 'placedAt'
      }, {
        title: 'Type',
        type: 'enum',
        term: 'type',
        suggestions: [
          { display: 'Backorder', value: 'backorder' },
          { display: 'Sellable', value: 'sellable' },
          { display: 'Preorder', value: 'preorder' },
          { display: 'Non-sellable', value: 'nonSellable' },
        ]
      }, {
        title: 'State',
        type: 'enum',
        term: 'state',
        suggestions: [
          { display: 'On Hand', value: 'hand' },
          { display: 'Hold', value: 'hold' },
          { display: 'Reserved', value: 'reserved' }
        ]
      }, {
        title: 'Previous',
        type: 'number',
        term: 'previous'
      }, {
        title: 'New',
        type: 'number',
        term: 'new'
      }, {
        title: 'Change',
        type: 'number',
        term: 'change'
      }, {
        title: 'New AFS',
        type: 'number',
        term: 'afs'
      }
    ]
  }, {
    title: 'Warehouse',
    type: 'object',
    options: [
      {
        title: 'Name',
        type: 'string',
        term: 'warehouse.name'
      }
    ]
  }
];

const { reducer, actions } = makeLiveSearch(
  'inventory.transactions',
  searchTerms,
  'transactions_search_view/_search',
  'inventoryScope'
);

export {
  reducer as default,
  actions
};
