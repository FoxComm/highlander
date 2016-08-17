import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Product',
    type: 'object',
    options: [
      {
        title: 'ID',
        type: 'identifier',
        term: 'id'
      }, {
        title: 'Name',
        type: 'string',
        term: 'product'
      }, {
        title: 'SKU',
        type: 'identifier',
        term: 'sku'
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
        term: 'type',
        suggestions: [
          { display: 'Sellable', value: 'Sellable' },
          { display: 'Non-sellable', value: 'Non-sellable' },
          { display: 'Preorder', value: 'Preorder' },
          { display: 'Backorder', value: 'Backorder' },
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
    initialState: { sortBy: 'product' },
    rawSorts: ['product']
  }
);

export {
  reducer as default,
  actions
};
