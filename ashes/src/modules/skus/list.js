import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'SKU',
    type: 'identifier',
    term: 'sku',
  }, {
    title: 'SKU Type',
    type: 'enum',
    term: 'type',
    suggestions: [
      { display: 'Sellable', value: 'Sellable' },
      { display: 'Non-sellable', value: 'Non-sellable' },
      { display: 'Preorder', value: 'Preorder' },
      { display: 'Backorder', value: 'Backorder' },
    ]
  }, {
    title: 'Warehouse',
    type: 'object',
    options: [
      {
        title: 'Name',
        type: 'string',
        term: 'stockLocation.name',
      }, {
        title: 'Type',
        type: 'string',
        term: 'stockLocation.type',
      }
    ],
  }, {
    title: 'OnHand',
    type: 'number',
    term: 'onHand',
  }, {
    title: 'OnHold',
    type: 'number',
    term: 'onHold'
  }, {
    title: 'Reserved',
    type: 'number',
    term: 'reserved',
  }, {
    title: 'AFS',
    type: 'number',
    term: 'afs',
  }, {
    title: 'AFS Cost',
    type: 'number',
    term: 'afsCost',
  }
];

const { reducer, actions } = makeLiveSearch(
  'skus.list',
  searchTerms,
  'inventory_search_view/_search',
  'inventoryScope',
  {
    initialState: { sortBy: '-createdAt' },
    rawSorts: ['product']
  }
);

export {
  reducer as default,
  actions
};
