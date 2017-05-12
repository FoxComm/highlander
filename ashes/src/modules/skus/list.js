/**
 * @flow
 */

import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'SKU : Code',
    type: 'identifier',
    term: 'skuCode',
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
  },
  {
    title: 'SKU : Is Archived',
    type: 'exists',
    term: 'archivedAt',
    suggestions: [
      { display: 'Yes', operator: 'exists' },
      { display: 'No', operator: 'missing' },
    ],
  },
];

export const rawSorts = ['title'];

const { reducer, actions } = makeLiveSearch(
  'skus.list',
  searchTerms,
  'sku_search_view/_search',
  'skusScope',
  {
    rawSorts,
  }
);

export {
  reducer as default,
  actions
};
