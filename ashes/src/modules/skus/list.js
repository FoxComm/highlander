/**
 * @flow
 */

import makeLiveSearch from '../live-search';

export type Sku = {
  id: number;
  image: string|null,
  context: string,
  skuCode: string,
  title: string,
  salePrice: string,
  salePriceCurrency: string,
  retailPrice: string,
  retailPriceCurrency: string,
};

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
