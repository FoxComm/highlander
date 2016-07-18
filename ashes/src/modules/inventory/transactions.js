import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Transaction',
    type: 'object',
    options: [
      {
        title: 'Date/Time',
        type: 'date',
        term: 'createdAt'
      }, {
        title: 'Event',
        type: 'string',
        term: 'event'
      }, {
        title: 'Warehouse',
        type: 'string',
        term: 'warehouse'
      }, {
        title: 'Type',
        type: 'enum',
        term: 'skuType',
        suggestions: [
          { display: 'Backorder', value: 'backorder' },
          { display: 'Preorder', value: 'preorder' },
          { display: 'Sellable', value: 'sellable' },
          { display: 'Non-sellable', value: 'nonSellable' },
        ]
      }, {
        title: 'State',
        type: 'enum',
        term: 'state',
        suggestions: [
          { display: 'On Hand', value: 'onHand' },
          { display: 'On Hold', value: 'onHold' },
          { display: 'Reserved', value: 'reserved' }
        ]
      }, {
        title: 'Previous',
        type: 'number',
        term: 'previousQuantity'
      }, {
        title: 'New',
        type: 'number',
        term: 'newQuantity'
      }, {
        title: 'Change',
        type: 'number',
        term: 'change'
      }, {
        title: 'New AFS',
        type: 'number',
        term: 'newAfs'
      }
    ]
  }
];

const { reducer, actions } = makeLiveSearch(
  'inventory.transactions',
  searchTerms,
  'inventory_transactions_search_view/_search',
  'inventoryScope',
  { skipInitialFetch: true }
);

export {
  reducer as default,
  actions
};
