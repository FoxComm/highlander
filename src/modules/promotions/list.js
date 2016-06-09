
import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Promotion : ID',
    type: 'number',
    term: 'id'
  },
  {
    title: 'Promotion : Name',
    type: 'string',
    term: 'promotionName'
  },
  {
    title: 'Promotion : Storefront Name',
    type: 'string',
    term: 'storefrontName'
  },
  {
    title: 'Promotion : Date/Time Created',
    type: 'date',
    term: 'createdAt'
  },
];

const { reducer, actions } = makeLiveSearch(
  'promotions.list',
  searchTerms,
  'promotions_search_view/_search',
  'promotionsScope',
  {
    initialState: { sortBy: '-createdAt' }
  }
);

export {
  reducer as default,
  actions
};
