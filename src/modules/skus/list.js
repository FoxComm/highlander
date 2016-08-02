/**
 * @flow
 */

import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'SKU : Code',
    type: 'identifier',
    term: 'code',
  },
  {
    title: 'SKU : Title',
    type: 'string',
    term: 'title',
  },
  {
    title: 'SKU : Price',
    type: 'currency',
    term: 'price'
  },
  {
    title: 'SKU : Archived At',
    type: 'date',
    term: 'archivedAt',
  }
];

const { reducer, actions } = makeLiveSearch(
  'skus.list',
  searchTerms,
  'sku_search_view/_search',
  'skusScope',
  {
    rawSorts: ['title']
  }
);

export {
  reducer as default,
  actions
};
