import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Product',
    type: 'object',
    options: [
      {
        title: 'ID',
        type: 'string',
        term: 'id'
      }, {
        title: 'Name',
        type: 'string',
        term: 'product'
      }, {
        title: 'SKU',
        type: 'string',
        term: 'code'
      }, {
        title: 'Product State',
        type: 'enum',
        term: 'productActive',
        suggestions: [
          { display: 'Active', value: 'true' },
          { display: 'Inactive', value: 'false' },
        ]
      }, {
        title: 'SKU State',
        type: 'enum',
        term: 'skuActive',
        suggestions: [
          { display: 'Active', value: 'true' },
          { display: 'Inactive', value: 'false' },
        ]
      }, {
        title: 'SKU Type',
        type: 'enum',
        term: 'skuType',
        suggestions: [
          { display: 'Backorder', value: 'backorder' },
          { display: 'Sellable', value: 'sellable' },
          { display: 'Preorder', value: 'preorder' },
          { display: 'Non-sellable', value: 'nonSellable' },
        ]
      }, {
        title: 'On Hand',
        type: 'number',
        term: 'onHand'
      }, {
        title: 'On Hold',
        type: 'number',
        term: 'onHold'
      }, {
        title: 'Reserved',
        type: 'number',
        term: 'reserved'
      }, {
        title: 'Safety Stock',
        type: 'number',
        term: 'safetyStock'
      }, {
        title: 'AFS',
        type: 'number',
        term: 'afs'
      }
    ]
  }
];

const { reducer, actions } = makeLiveSearch(
  'inventory.list',
  searchTerms,
  'inventory_search_view/_search',
  'inventoryScope',
  {
    initialState: { sortBy: 'product' }
  }
);

export {
  reducer as default,
  actions
};
